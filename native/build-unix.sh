#!/usr/bin/env bash
# Builds libusb and libbladeRF as shared libraries for the host platform and
# copies them to dist/native/<platform>. Works on Linux and macOS.
#
# Usage: native/build-unix.sh [bladeRF-git-ref]
#
# Both libraries stay separate shared libraries because both are LGPL-2.1, which
# lets users replace them. libusb is built without udev so the Linux binaries do
# not need libudev at runtime.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=versions.env
source "$ROOT/native/versions.env"
REF="${1:-$BLADERF_REF}"
WORK="${JBLADERF_WORK_DIR:-$ROOT/build/native-work}"
JOBS="$(getconf _NPROCESSORS_ONLN)"

case "$(uname -s)" in
    Linux)  OS=linux ;;
    Darwin) OS=macos ;;
    *) echo "Unsupported OS: $(uname -s)" >&2; exit 1 ;;
esac
case "$(uname -m)" in
    x86_64|amd64)  ARCH=x86_64 ;;
    aarch64|arm64) ARCH=aarch64 ;;
    *) echo "Unsupported architecture: $(uname -m)" >&2; exit 1 ;;
esac
PLATFORM="$OS-$ARCH"
OUT="$ROOT/dist/native/$PLATFORM"
LICENSES="$ROOT/dist/native/licenses"

rm -rf "$WORK" "$OUT"
mkdir -p "$WORK" "$OUT" "$LICENSES/bladeRF"
cd "$WORK"

echo "=== libusb $LIBUSB_VERSION ==="
TARBALL="libusb-$LIBUSB_VERSION.tar.bz2"
curl -fsSL -o "$TARBALL" \
    "https://github.com/libusb/libusb/releases/download/v$LIBUSB_VERSION/$TARBALL"
if command -v sha256sum >/dev/null; then
    echo "$LIBUSB_TARBALL_SHA256  $TARBALL" | sha256sum -c -
else
    echo "$LIBUSB_TARBALL_SHA256  $TARBALL" | shasum -a 256 -c -
fi
tar xjf "$TARBALL"

MIN_MACOS=11.0
USB_PREFIX="$WORK/usb"
USB_ARGS=(--prefix="$USB_PREFIX" --disable-static --enable-shared)
if [ "$OS" = linux ]; then
    USB_ARGS+=(--disable-udev)
else
    export CFLAGS="-O2 -mmacosx-version-min=$MIN_MACOS"
    export LDFLAGS="-mmacosx-version-min=$MIN_MACOS"
fi
(cd "libusb-$LIBUSB_VERSION" && ./configure "${USB_ARGS[@]}" >/dev/null && make -j"$JOBS" install >/dev/null)
unset CFLAGS LDFLAGS

echo "=== libbladeRF $REF ==="
git -c advice.detachedHead=false clone --quiet --depth 1 --branch "$REF" "$BLADERF_REPOSITORY" bladeRF
# The AD9361 driver is the only submodule that libbladeRF needs.
git -C bladeRF submodule --quiet update --init --depth 1 thirdparty/analogdevicesinc/no-OS
UPSTREAM_COMMIT="$(git -C bladeRF rev-parse HEAD)"
# Only the library is built. Skipping the utilities avoids their submodule and
# their optional dependencies, which differ on each platform.
: > bladeRF/host/utilities/CMakeLists.txt

# Parts of a version line such as: set(VERSION_INFO_MINOR  6)
version_part() {
    sed -nE "s/^set\(VERSION_INFO_$1 +([0-9]+)\)/\1/p" bladeRF/host/libraries/libbladeRF/CMakeLists.txt
}
UPSTREAM_VERSION="$(version_part MAJOR).$(version_part MINOR).$(version_part PATCH)"
if [ "$REF" = "$BLADERF_REF" ] && [ "$UPSTREAM_VERSION" != "$BLADERF_VERSION" ]; then
    echo "$REF contains libbladeRF $UPSTREAM_VERSION, not BLADERF_VERSION $BLADERF_VERSION" >&2
    exit 1
fi

# The libusb hints replace upstream's search, which would otherwise find the
# copy that a package manager installed. pkg-config is disabled for that reason.
# The version check is skipped because it runs a program, and the value is given
# directly so the features that depend on it stay enabled.
if [ "$OS" = linux ]; then
    USB_LIB="$USB_PREFIX/lib/libusb-1.0.so"
else
    USB_LIB="$USB_PREFIX/lib/libusb-1.0.0.dylib"
fi
CMAKE_ARGS=(
    -S bladeRF/host -B build-bladerf
    -DCMAKE_BUILD_TYPE=Release
    -DCMAKE_DISABLE_FIND_PACKAGE_PkgConfig=ON
    -DLIBUSB_HEADER_FILE="$USB_PREFIX/include/libusb-1.0/libusb.h"
    -Dusb_LIBRARY="$USB_LIB"
    -DLIBUSB_SKIP_VERSION_CHECK=ON
    -DLIBUSB_VERSION="$LIBUSB_VERSION"
    -DTREAT_WARNINGS_AS_ERRORS=OFF
    -DTEST_LIBBLADERF=OFF
    # Keeps the build tree's paths out of the library's assertion messages.
    -DCMAKE_C_FLAGS="-ffile-prefix-map=$WORK=."
)
if [ "$REF" = "$BLADERF_REF" ]; then
    # Drops the -git suffix from the version the library reports.
    CMAKE_ARGS+=(-DVERSION_INFO_EXTRA=)
fi
if [ "$OS" = linux ]; then
    # $ORIGIN only: the build tree's paths must not leak into the shipped library.
    CMAKE_ARGS+=(-DCMAKE_BUILD_WITH_INSTALL_RPATH=ON -DCMAKE_INSTALL_RPATH='$ORIGIN')
else
    CMAKE_ARGS+=(-DCMAKE_OSX_DEPLOYMENT_TARGET="$MIN_MACOS")
fi
cmake "${CMAKE_ARGS[@]}" >/dev/null
cmake --build build-bladerf --parallel "$JOBS" --target libbladerf_shared >/dev/null

echo "=== packaging $PLATFORM ==="
if [ "$OS" = linux ]; then
    cp -L "$USB_PREFIX/lib/libusb-1.0.so.0" "$OUT/libusb-1.0.so.0"
    cp -L build-bladerf/output/libbladeRF.so.2 "$OUT/libbladeRF.so.2"
    chmod 755 "$OUT"/*
    # Only glibc and libusb may remain as dependencies.
    readelf -d "$OUT/libbladeRF.so.2" | grep -E 'NEEDED|RUNPATH|RPATH'
else
    cp -L "$USB_PREFIX/lib/libusb-1.0.0.dylib" "$OUT/libusb-1.0.0.dylib"
    cp -L build-bladerf/output/libbladeRF.2.dylib "$OUT/libbladeRF.2.dylib"
    chmod 755 "$OUT"/*
    # Point libbladeRF at the bundled libusb, not the absolute build path, and
    # re-sign because install_name_tool invalidates the ad-hoc signatures.
    install_name_tool -id @rpath/libusb-1.0.0.dylib "$OUT/libusb-1.0.0.dylib"
    install_name_tool -id @rpath/libbladeRF.2.dylib "$OUT/libbladeRF.2.dylib"
    install_name_tool -change "$USB_LIB" @loader_path/libusb-1.0.0.dylib "$OUT/libbladeRF.2.dylib"
    codesign -f -s - "$OUT"/*.dylib
    otool -L "$OUT/libbladeRF.2.dylib"
fi

# Pinned sources must match the bindings. Tracking upstream master only warns, so
# that one new upstream function does not stop the nightly builds.
if ! "$ROOT/native/check-bindings.sh" bladeRF/host/libraries/libbladeRF/include; then
    [ "$REF" != "$BLADERF_REF" ] || exit 1
    echo "::warning::Bindings differ from the libbladeRF headers at $REF"
fi

cp bladeRF/COPYING "$LICENSES/bladeRF/COPYING"
cp bladeRF/legal/licenses/* "$LICENSES/bladeRF/"
# no-OS has no license file: its terms are at the top of each source file.
sed '/\*\*\*\*\*\*\*\*\/$/q' bladeRF/thirdparty/analogdevicesinc/no-OS/ad9361/sw/ad9361.c \
    > "$LICENSES/ad9361-LICENSE.txt"
cp "libusb-$LIBUSB_VERSION/COPYING" "$LICENSES/libusb-COPYING"
cat > "$OUT/build-info.properties" <<INFO
bladerf.ref=$REF
bladerf.commit=$UPSTREAM_COMMIT
bladerf.version=$UPSTREAM_VERSION
libusb.version=$LIBUSB_VERSION
INFO
ls -l "$OUT"
