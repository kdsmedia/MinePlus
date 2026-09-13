#!/usr/bin/env bash
#
# Build recipe for the bundled native `x11miner` executable.
#
# Produces real, distribution-ready X11 + Stratum miners for Android:
#   out/arm64-v8a/x11miner
#   out/armeabi-v7a/x11miner
# and copies them into ../app/src/main/jniLibs/<abi>/x11miner
#
# Source: cpuminer-multi (https://github.com/tpruvot/cpuminer-multi)
#   - algorithm: x11 (Dash lineage), same sphlib core already vendored in repo
#   - protocol:  stratum+tcp / stratum+ssl
#   - licence:   GPLv2 (redistributable)
#
# Prerequisites:
#   - Android NDK (r25+)
#   - cpuminer-multi source checked out at $SRC (default ./cpuminer-multi)
#
set -euo pipefail

NDK="${NDK:-$ANDROID_HOME/ndk/$(ls "$ANDROID_HOME/ndk" 2>/dev/null | sort -V | tail -1)}"
[ -n "${NDK:-}" ] || { echo "Set NDK or ANDROID_HOME first"; exit 1; }
TC="$NDK/toolchains/llvm/prebuilt/linux-x86_64"
SRC="${SRC:-$PWD/cpuminer-multi}"
OUT="$PWD/out"
mkdir -p "$OUT"

build_abi() {
  local cc_arch="$1"; local cc_dir="$2"; local outdir="$OUT/$2"; local pieflag="$3"
  local cc
  case "$cc_arch" in
    arm64-v8a)  cc="$TC/bin/aarch64-linux-android26-clang" ;;
    armeabi-v7a) cc="$TC/bin/armv7a-linux-androideabi26-clang" ;;
    *) echo "Unknown ABI: $cc_arch"; exit 1 ;;
  esac
  [ -x "$cc" ] || { echo "Missing clang: $cc"; exit 1; }
  echo "==> Building $cc_arch"
  "$cc" \
    --sysroot="$TC/sysroot" \
    $pieflag \
    -O3 -fPIE -fvisibility=hidden \
    -I"$SRC" \
    $(find "$SRC" -name '*.c' ! -name '*windows*' ! -name '*test*' | tr '\n' ' ') \
    -o "$outdir/x11miner" \
    -lpthread -lm -lz 2> "$outdir/build.log"
  chmod +x "$outdir/x11miner"
  echo "    -> $outdir/x11miner"
}

# Dynamic linkers on the device need -pie for modern Android.
build_abi arm64-v8a   arm64-v8a   "-pie"
build_abi armeabi-v7a armeabi-v7a "-pie"

echo "==> Copying into app/src/main/jniLibs and assets/x11miner"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
for abi in arm64-v8a armeabi-v7a; do
  mkdir -p "$ROOT/app/src/main/jniLibs/$abi" "$ROOT/app/src/main/assets/x11miner/$abi"
  cp "$OUT/$abi/x11miner" "$ROOT/app/src/main/jniLibs/$abi/x11miner"
  cp "$OUT/$abi/x11miner" "$ROOT/app/src/main/assets/x11miner/$abi/x11miner"
  chmod +x "$ROOT/app/src/main/jniLibs/$abi/x11miner" \
          "$ROOT/app/src/main/assets/x11miner/$abi/x11miner"
done
echo "Done."