# Native X11 miner binaries

This directory holds the real, redistributable `x11miner` executable for each
Android ABI. **Nothing here may be a fake placeholder.**

```
app/src/main/jniLibs/
├── arm64-v8a/
│   └── x11miner        <- native executable (ELF), executable bit set
└── armeabi-v7a/
    └── x11miner
```

## Requirements for the bundled miner

A binary placed here must satisfy all of the following:

| Requirement | Check |
|---|---|
| Built for Android (Bionic libc, not glibc) | `readelf -l x11miner \| grep interpreter` shows `/system/bin/linker64` (or `linker`); runs on Android ≥ API 26 |
| ABI matches the directory | `readelf -A x11miner` lists `AArch64` for `arm64-v8a`; `Tag_CPU_arch: ARM v7` for `armeabi-v7a` |
| Executable permission set | `ls -l x11miner` shows `-rwxr-xr-x` |
| Supports X11 (Dash ASIC-resistant PoW) | binary accepts `--algo=x11` / `-a x11` |
| Supports Stratum protocol | binary accepts `stratum+tcp://` / `stratum+ssl://` URLs |
| Works with NiceHash X11 auto endpoint | tested against `x11.auto.nicehash.com:9200/443` with `USERNAME.RIG` login |
| Can redistribute inside an APK | source is permissively licensed (e.g. GPLv2 as used by `cpuminer`), license text bundled |

## Recommended source: cpuminer-multi (MIT/X11 + GPLv2)

`cpuminer-multi` (https://github.com/tpruvot/cpuminer-multi) supports
`-a x11`, multiple Stratum variants and ships the identical sphlib X11
implementation already present under `src/main/jni/X11/hash` (same `sph_*`
authors, MIT/public-domain sphlib core). It is freely redistributable.

Build with Android NDK:

```sh
# 1. Install Android NDK (e.g. r25 or newer) and set a standalone toolchain.
export NDK=/path/to/android-ndk
export TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/linux-x86_64

# 2. arm64-v8a
$TOOLCHAIN/bin/aarch64-linux-android26-clang \
  --sysroot=$TOOLCHAIN/sysroot \
  -O3 -marm -mfpu=neon -fPIE -pie -static?   # choose static-linking as needed
  cpuminer-multi/*.c ... -o x11miner

# 3. armeabi-v7a
$TOOLCHAIN/bin/armv7a-linux-androideabi26-clang ... -o x11miner
```

For a static binary add `-static-libgcc` and link `libc.a`/`libm.a` from the
sysroot so the executable needs no runtime linker dependencies.

## Licence

- sphlib core (already vendored in this repo): MIT / public domain.
- cpuminer-multi: GPLv2 — when bundling a binary derived from it, ship the
  GPLv2 license text in the APK (`assets/licenses/` recommended) as required.

## Verification script

`native/build_recipe.sh` (repo root `native/`) documents the exact toolchain
commands; `native/verify_x11miner.sh` validates ABI, executable bit, X11 and
Stratum support of any binary placed here before shipping.