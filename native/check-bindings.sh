#!/usr/bin/env bash
# Compares the functions in the libbladeRF headers with the bindings in
# Native.java, so a new upstream function fails the build rather than being
# silently unavailable.
#
# Usage: native/check-bindings.sh <libbladeRF-include-directory>
set -euo pipefail
export LC_ALL=C  # sort has to agree with comm

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INCLUDE="$1"
BINDINGS="$ROOT/src/main/java/io/github/dsheirer/bladerf/internal/Native.java"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# Functions that upstream documents as deprecated, which are left unbound.
cat > "$WORK/deprecated" <<'LIST'
bladerf_dac_read
bladerf_dac_write
bladerf_get_lna_gain
bladerf_get_rxvga1
bladerf_get_rxvga2
bladerf_get_serial
bladerf_get_txvga1
bladerf_get_txvga2
bladerf_set_lna_gain
bladerf_set_rxvga1
bladerf_set_rxvga2
bladerf_set_txvga1
bladerf_set_txvga2
LIST
sort -o "$WORK/deprecated" "$WORK/deprecated"

# Declarations start with API_EXPORT, and the name can be up to two lines later.
for header in libbladeRF.h bladeRF1.h bladeRF2.h; do
    grep -A2 '^API_EXPORT' "$INCLUDE/$header"
done | grep -oE '\bbladerf_[a-z0-9_]+\(' | tr -d '(' | sort -u > "$WORK/all"
comm -23 "$WORK/all" "$WORK/deprecated" > "$WORK/header"
grep -oE '"bladerf_[a-z0-9_]+"' "$BINDINGS" | tr -d '"' | sort -u > "$WORK/bindings"

MISSING="$(comm -23 "$WORK/header" "$WORK/bindings")"
STALE="$(comm -13 "$WORK/header" "$WORK/bindings")"
if [ -n "$MISSING" ] || [ -n "$STALE" ]; then
    [ -z "$MISSING" ] || printf 'Functions in the headers without a binding:\n%s\n' "$MISSING" >&2
    [ -z "$STALE" ] || printf 'Bindings not in the headers:\n%s\n' "$STALE" >&2
    exit 1
fi
echo "Bindings match the libbladeRF headers ($(wc -l < "$WORK/header" | tr -d ' ') functions)"
