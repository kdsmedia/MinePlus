package com.altomedia.mineplus.model

/** Lifecycle states the miner can be in. */
enum class MinerStatus {
    STOPPED,
    CONNECTING,
    CONNECTED,
    MINING,
    ERROR
}

/**
 * Snapshot of everything the UI needs to render the dashboard.
 * Emitted by a StateFlow from the service / ViewModel.
 */
data class MinerState(
    val status: MinerStatus = MinerStatus.STOPPED,
    val hashrate: Double = 0.0,          // hashes per second (H/s)
    val acceptedShares: Long = 0L,
    val rejectedShares: Long = 0L,
    val uptimeSeconds: Long = 0L,
    val difficulty: Double = 0.0,
    val poolEndpoint: String = "",
    val lastError: String? = null,
    val connected: Boolean = false
) {
    val totalShares: Long get() = acceptedShares + rejectedShares
}