package com.altomedia.mineplus.model

import java.util.concurrent.atomic.AtomicLong

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

/** A single line kept in the in-app miner log. */
data class MinerLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val message: String = ""
) {
    companion object {
        private val counter = AtomicLong(0)
    }

    val id: Long = counter.incrementAndGet()
}