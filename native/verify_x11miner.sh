#!/usr/bin/env bash
#
# Validates an Android X11 miner binary before it ships.
# Usage: verify_x11miner.sh <path-to-x11miner> <abi: arm64-v8a|armeabi-v7a>
#
# Fails (non-zero) if the binary is a fake placeholder or violates any of
# the native-miner requirements listed in app/src/main/jniLibs/README.md.
set -u

BIN="${1:?path to x11miner required}"
ABI="${2:-arm64-v8a}"
failures=0

check() { # check <desc> <cmd...>
  local desc="$1"; shift
  if "$@" >/dev/null 2>&1; then echo "  [ok]    $desc"; else echo "  [FAIL]  $desc"; failures=$((failures+1)); fi
}

[ -f "$BIN" ]      || { echo "Missing file: $BIN"; exit 2; }
[ -x "$BIN" ]      || { echo "Not executable: $BIN"; exit 2; }

echo "==> Verifying $BIN ($ABI)"

if command -v readelf >/dev/null; then
  check "ELF binary" readelf -h "$BIN"
  check "Android linker64 interpreter (arm64) / linker (armv7)" grep -Eq "interpreter: (/system/bin/linker64|/system/bin/linker)" < <(readelf -l "$BIN" 2>/dev/null)
  if [ "$ABI" = "arm64-v8a" ]; then
    check "AArch64 machine type" grep -q "AArch64" < <(readelf -h "$BIN")
  else
    check "ARM machine type" grep -q "ARM" < <(readelf -h "$BIN")
  fi
else
  echo "  [warn]  readelf not installed - skipping ELF checks"
fi

strings_bin="strings"
if command -v "$strings_bin" >/dev/null; then
  check "Supports X11 algorithm (-a x11 / --algo=x11)" \
    grep -Eqm1 "x11" < <("$strings_bin" "$BIN")
  check "Supports Stratum protocol" grep -Eqm1 "stratum" < <("$strings_bin" "$BIN")
  check "NiceHash auto endpoint reference" grep -Eqm1 "nicehash" < <("$strings_bin" "$BIN")
else
  echo "  [warn]  strings not installed - skipping capability probes"
fi

# Size sanity: a 3 KB placeholder is not a miner.
size=$(wc -c < "$BIN")
echo "  [info]  size = $size bytes"
if [ "$size" -lt 50000 ]; then
  echo "  [FAIL]  binary too small to be a real miner ($size bytes)"
  failures=$((failures+1))
fi

if [ "$failures" -gt 0 ]; then
  echo "==> FAILED ($failures problem(s))"
  exit 1
fi
echo "==> OK: $BIN looks like a real redistributable X11/Stratum Android miner"
exit 0