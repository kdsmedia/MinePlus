package com.altomedia.mineplus.model

/**
 * A single mining job handed out by the stratum server via `mining.notify`.
 *
 * For X11 the block header is 76 bytes; the last field (nonce) is left as
 * 32 zeroes space, the miner stamps bytes [72..75] little-endian.
 */
data class StratumWork(
    val jobId: String,
    val prevHash: String,        // 32-byte big-endian hex
    val coinb1: String,
    val coinb2: String,
    val merkleBranch: List<String>,
    val version: Long,
    val nbits: String,
    val ntime: String,
    val cleanJobs: Boolean,
    val difficulty: Double = 1.0
)