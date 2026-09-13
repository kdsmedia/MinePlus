package com.altomedia.mineplus.model

/**
 * Immutable copy of the pool / worker configuration used to start the miner.
 */
data class MinerSettings(
    val algorithm: String = "X11",
    val host: String = "x11.auto.nicehash.com",
    val port: Int = 443,
    val useSsl: Boolean = true,
    val walletAddress: String = "",
    val rigName: String = "ANDROID01",
    val password: String = "",
    val autoReconnect: Boolean = true,
    val reconnectIntervalSec: Int = 10,
    val maxReconnects: Int = 5,
    val backgroundMining: Boolean = true,
    val startOnBoot: Boolean = false,
    val threads: Int = 1,
    // Battery protection
    val minBatteryLevel: Int = 20,        // stop mining below this %
    val stopWhenCharging: Boolean = false, // stop when the device is charging
    val stopTempC: Int = 70,              // stop when temperature reaches this °C
    val reduceIntensity: Boolean = true   // reduce intensity when conditions get unsafe
) {
    companion object {
        /** Selectable reconnect intervals, in seconds. */
        val RECONNECT_INTERVALS: List<Int> = listOf(5, 10, 20, 30, 60)
    }
    val endpoint: String get() = (if (useSsl) "stratum+ssl" else "stratum+tcp") + "://$host:$port"

    /** NiceHash style worker login. For BTC payout + X11 pool name. */
    val login: String get() = if (walletAddress.isBlank()) "" else walletAddress + "." + rigName
}