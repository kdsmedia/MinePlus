cmake_minimum_required(VERSION 3.22.1)
project(mineplus_x11 LANGUAGES C CXX)

# Native X11 engine used by the Stratum miner.
add_library(mineplus_x11 SHARED
        jni_wrapper.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/../jni/X11/hash/sph_blake.c
        ${CMAKE_CURRENT_SOURCE_DIR}/../jni/X11/hash/sph_bmw.c
        ${CMAKE_CURRENT_SOURCE_DIR}/../jni/X11/hash/sph_cubehash.c
        ${CMAKE_CURRENT_SOURCE_DIR}/../jni/X11/hash/sph_echo.c
        ${CMAKE_CURRENT_SOURCE_DIR}/../jni/X11/hash/sph_groestl.c
        ${CMAKE_CURRENT_SOURCE_DIR}/../jni/X11/hash/sph_jh.c
        ${CMAKE_CURRENT_SOURCE_DIR}/../jni/X11/hash/sph_keccak.c
        ${CMAKE_CURRENT_SOURCE_DIR}/../jni/X11/hash/sph_luffa.c
        ${CMAKE_CURRENT_SOURCE_DIR}/../jni/X11/hash/sph_shavite.c
        ${CMAKE_CURRENT_SOURCE_DIR}/../jni/X11/hash/sph_simd.c
        ${CMAKE_CURRENT_SOURCE_DIR}/../jni/X11/hash/sph_skein.c
        ${CMAKE_CURRENT_SOURCE_DIR}/../jni/X11/hash/x11hash.c
)

target_include_directories(mineplus_x11 PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/../jni/X11/hash
)

target_compile_options(mineplus_x11 PRIVATE
        -O3
        -fvisibility=hidden
        -Wall -Wextra
)

find_library(log-lib log)
target_link_libraries(mineplus_x11 PRIVATE ${log-lib} android)