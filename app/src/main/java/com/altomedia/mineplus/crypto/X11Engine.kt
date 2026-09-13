package com.altomedia.mineplus.crypto

import kotlin.math.min

/**
 * X11 proof-of-work digest used by the NiceHash X11 pool.
 *
 * The heavy lifting is done by the native `mineplus_x11` library
 * (built from the C engine under `src/main/jni/X11/hash`). The alias
 * factor is standard X11 byte order: the first 4 bytes of the nonce space
 * are folded in big-endian order.
 */
object X11Engine {

    init {
        System.loadLibrary("mineplus_x11")
    }

    private external fun x11HashNative(data: ByteArray): ByteArray

    /** Computes the 32-byte X11 digest of [data]. */
    fun x11Hash(data: ByteArray): ByteArray = x11HashNative(data)

    /**
     * Hashes a 32-byte block header (big-endian) and returns a digest array
     * where we compare the full 8 words in native (big-endian) order.
     */
    fun x11Hash(header: UByteArray): ByteArray {
        val raw = ByteArray(header.size)
        for (i in header.indices) raw[i] = header[i].toByte()
        return x11Hash(raw)
    }

    /** Throttled hashing loop helper used by the miner core. */
    fun hashBlockHeader(header: ByteArray, nonceStart: ULong, nonceEnd: ULong, output: (ByteArray, ULong) -> Unit): ULong {
        var nonce = nonceStart
        var hashes = 0UL
        // cache a mutable copy so we can stamp the nonce without reallocating
        val block = header.copyOf()
        while (nonce < nonceEnd && hashes < 10_000_000UL) {
            writeNonceLe(block, nonce)
            val digest = x11Hash(block)
            output(digest, nonce)
            nonce += 1UL
            hashes += 1UL
        }
        return hashes
    }

    /** Writes a little-endian nonce into the last 4 bytes of a block header. */
    fun writeNonceLe(block: ByteArray, nonce: ULong) {
        val n = nonce.toLong()
        val base = block.size - 4
        if (base < 0) return
        block[base] = (n and 0xFF).toByte()
        block[base + 1] = ((n shr 8) and 0xFF).toByte()
        block[base + 2] = ((n shr 16) and 0xFF).toByte()
        block[base + 3] = ((n shr 24) and 0xFF).toByte()
    }
}