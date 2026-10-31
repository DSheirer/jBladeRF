# jBladeRF

Java bindings for [libbladeRF](https://github.com/Nuand/bladeRF), the driver for
Nuand bladeRF software-defined radios, built on the Java Foreign Function & Memory
API. The jar contains the native libraries, so there is nothing else to install.

| OS | x86-64 | aarch64 |
|---------|----|----|
| Linux (glibc 2.34+, such as Ubuntu 22.04, Debian 12, RHEL 9) | ✓ | ✓ |
| macOS 11+ | ✓ | ✓ |
| Windows | ✓ | ✓ |

Requires Java 23 or later.

## Getting it

Download `jbladerf-<version>.jar` from
[Releases](https://github.com/DSheirer/jBladeRF/releases). The rolling
[`nightly`](https://github.com/DSheirer/jBladeRF/releases/tag/nightly)
pre-release tracks `main` of this project and `master` of upstream.

Check that your machine can see a device:

```
java -jar jbladerf-<version>.jar
```

## Using it

```java
try (BladeRfDevice device = BladeRf.open()) {
    device.setFrequency(Channel.RX0, 915_000_000L);
    device.setSampleRate(Channel.RX0, 2_000_000);
    device.setBandwidth(Channel.RX0, 1_500_000);

    // Enables the channels, and disables them when the stream ends.
    try (BladeRfStream stream = device.startRx(ChannelLayout.RX_X1, SampleFormat.SC16_Q11,
            StreamConfig.DEFAULT, buffer -> {
                short[] iq = buffer.copyShorts();   // interleaved I, Q
                queue.offer(iq);
                return true;                        // false stops the stream
            })) {
        Thread.sleep(10_000);
    }
}
```

The synchronous interface is simpler if you can dedicate a thread to it:

```java
device.syncConfig(ChannelLayout.RX_X1, SampleFormat.SC16_Q11, 16, 8192, 8, 3500);
device.enableChannel(Channel.RX0, true);
try (Arena arena = Arena.ofConfined()) {
    MemorySegment samples = arena.allocate(SampleFormat.SC16_Q11.byteLength(8192), 2);
    while (running) {
        device.syncRx(samples, 8192, null, 3500);
        process(samples);
    }
}
device.enableChannel(Channel.RX0, false);
```

`SampleListener` and `SampleProducer` run on a callback from the driver, which
times out if it is held up, so hand slow work to another thread. `buffer.samples()`
gives zero-copy access, valid only until the listener returns; later use throws an
exception rather than reading memory that is being reused. An exception thrown by
a listener stops the stream and is available from `stream.failure()`.

Channels of the same direction share a sample rate on a bladeRF 2.0, so setting one
changes the other.

Methods that only one board supports are labelled in the Javadoc, and fail with
`BladeRfError.UNSUPPORTED` on the other.

### FPGA

A bladeRF needs an FPGA bitstream before it can stream, and the bitstream is not
part of this jar. If `isFpgaConfigured()` returns false, load one with
`device.loadFpga(path)`, or store one on the device with `device.flashFpga(path)`.
Bitstreams are [published by Nuand](https://www.nuand.com/fpga_images/), and they
have to suit the device and the version of libbladeRF.

libbladeRF also loads a bitstream by itself when opening a device, if it finds one
such as `hostedxA4.rbf` in its search path: the `BLADERF_SEARCH_DIR` environment
variable, the current directory, `~/.config/Nuand/bladeRF/` or
`/usr/share/Nuand/bladeRF/`. On Windows, use `%APPDATA%\Nuand\bladeRF\`.

### Native access

Java prints a warning, which future versions will turn into an error, unless
you allow native access:

```
java --enable-native-access=ALL-UNNAMED -cp app.jar:jbladerf.jar my.App
```

On the module path, use `--enable-native-access=io.github.dsheirer.bladerf`. In
your own executable jar, add `Enable-Native-Access: ALL-UNNAMED` to the manifest.

### USB access

- **Linux:** install upstream's
  [udev rules](https://github.com/Nuand/bladeRF/tree/master/host/misc/udev)
  to use a device without root.
- **Windows:** the jar uses the libusb backend, so the device needs the WinUSB
  driver and not the Cypress driver from Nuand's installer. A tool such as
  [Zadig](https://zadig.akeo.ie/) can switch it.

### Native library location

Libraries are extracted to the temporary directory at startup. Set
`-Djbladerf.library.path=<dir>` to load them from a directory instead: for
example, to supply your own build of libbladeRF or libusb, or if the temporary
directory is mounted `noexec`.

## Building

You need JDK 23+, CMake, a C compiler, make, patch and git. On Windows use Visual
Studio, 7-Zip and Git for Windows, which provides patch.

```
./native/build-unix.sh                                # Linux and macOS
pwsh native/build-windows.ps1 -Arch x64               # Windows; or -Arch ARM64
./gradlew build                                       # jar in build/libs
./gradlew test -PhardwareTest=true                    # also test a connected device
```

A local jar contains only your platform. The hardware tests are receive-only, and
they need a device with its FPGA loaded.

## Releases

jBladeRF versions follow upstream: jBladeRF 2.6.0 wraps libbladeRF 2.6.0. The
upstream release is pinned in [`native/versions.env`](native/versions.env), which
is the only place the version is set.

| Trigger | What happens |
|---------|--------------|
| Pull request | Builds and tests all six platforms; publishes nothing |
| Merge to `main` | Replaces the `nightly` pre-release, built against upstream `master` |
| Daily schedule | The same, if this project or upstream has changed |
| Tag `vX.Y.Z` | Publishes a release built from the pinned upstream version |

To release, tag the pinned version: `git tag v2.6.0 && git push origin v2.6.0`.
The workflow rejects a tag that does not match the pin. For a Java-only fix on
the same upstream version add a fourth number, such as `v2.6.0.1`; for a
pre-release add a suffix, such as `v2.6.0-rc1`.

To adopt a new upstream release, change `BLADERF_REF` to its tag and
`BLADERF_VERSION` to the libbladeRF version that it contains. The build fails if
the two disagree. If upstream added functions, the build lists them; add them to
`Native.java` and any new structure fields to `Layouts.java`, and update the
values in `LibraryTest.structuresMatchCompiler` if structures changed.

## License

Apache License 2.0. The bundled native libraries have their own licenses: see
[THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
