package com.altomedia.mineplus.miner

/**
 * JNI entry point into the native mining loop.
 *
 * The heavy nonce loop lives in C so that the JNI boundary is crossed once
 * per chunk (e.g. 262,144 nonces) instead of once per hash. On every hash
 * whose digest is <= target, the native code calls back into
 * [ShareCallback.onShareFound].
 */
object NativeMiner {

    private external fun mineNative(
        header: ByteArray,
        target: ByteArray,
        nonceStart: Long,
        nonceCount: Long,
        callback: ShareCallback
    ): Long

    /** Called from native code on the mining thread for every valid share. */
    interface ShareCallback {
        fun onShareFound(nonce: Long, digest: ByteArray)
    }

    /**
     * Mines [count] nonces starting at [nonceStart] against [target]
     * (32-byte big-endian). Returns the number of hashes computed.
     */
    fun mine(
        header: ByteArray,
        target: ByteArray,
        nonceStart: Long,
        count: Long,
        onShare: (Long, ByteArray) -> Unit
    ): Long {
        val callback = object : ShareCallback {
            override fun onShareFound(nonce: Long, digest: ByteArray) {
                onShare(nonce, digest)
            }
        }
        return mineNative(header, target, nonceStart, count, callback)
    }
}