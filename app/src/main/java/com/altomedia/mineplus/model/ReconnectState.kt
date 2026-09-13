package com.altomedia.mineplus.model

/**
 * Live view of the auto-reconnect loop, exposed to the UI so it can render
 * "Connection lost", "Retrying in X seconds…" and final failure states.
 */
data class ReconnectState(
    val idle: Boolean = true,
    val retries: Int = 0,
    val nextRetryInSec: Int = 0,
    val maxRetries: Int = 5,
    val message: String? = null,
    val gaveUp: Boolean = false
) {
    val reconnecting: Boolean get() = !idle && !gaveUp
}