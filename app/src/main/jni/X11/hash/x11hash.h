#ifndef MINEPLUS_X11HASH_H
#define MINEPLUS_X11HASH_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stddef.h>

/**
 * Computes the X11 proof-of-work hash (used by NiceHash / Dash):
 *
 *   blake -> bmw -> groestl -> skein -> jh -> keccak ->
 *   luffa -> cubehash -> shavite -> simd -> echo
 *
 * @param input  message bytes
 * @param len    input length in bytes
 * @param output 32-byte digest (exactly 32 bytes)
 */
void x11_hash(const unsigned char *input, size_t len,
              unsigned char output[32]);

#ifdef __cplusplus
}
#endif

#endif /* MINEPLUS_X11HASH_H */