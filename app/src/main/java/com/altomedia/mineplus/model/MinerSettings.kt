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
    val rigName: String = "android_worker",
    val password: String = "",
    val autoReconnect: Boolean = true,
    val backgroundMining: Boolean = true,
    val startOnBoot: Boolean = false,
    val threads: Int = 1
) {
    val endpoint: String get() = (if (useSsl) "stratum+ssl" else "stratum+tcp") + "://$host:$port"

    /** NiceHash style worker login. For BTC payout + X11 pool name. */
    val login: String get() = if (walletAddress.isBlank()) "" else walletAddress + "." + rigName
}