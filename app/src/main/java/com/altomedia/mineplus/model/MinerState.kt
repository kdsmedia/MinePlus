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
    val averageHashrate: Double = 0.0,   // rolling average H/s
    val acceptedShares: Long = 0L,
    val rejectedShares: Long = 0L,
    val uptimeSeconds: Long = 0L,
    val difficulty: Double = 0.0,
    val poolEndpoint: String = "",
    val lastError: String? = null,
    val connected: Boolean = false,
    val sharesPerHour: Double = 0.0,
    val lastShareSecondsAgo: Long = Long.MAX_VALUE,
    val hashrateHistory: List<Double> = emptyList()  // samples for the chart
) {
    val totalShares: Long get() = acceptedShares + rejectedShares
    val rejectRatePercent: Double get() =
        if (totalShares == 0L) 0.0 else (rejectedShares * 100.0) / totalShares
}