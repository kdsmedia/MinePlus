package com.altomedia.mineplus.model

/**
 * Snapshot of available on-device sensor/hardware stats, emitted by the
 * device monitor so the UI never reads sensors directly.
 */
data class DeviceStats(
    val cpuTemperatureC: Float? = null,     // null when no sensor available
    val batteryPercent: Int = -1,           // -1 when unavailable
    val cpuUsagePercent: Float = -1f,       // -1 when unknown
    val memoryUsedMB: Long = 0,
    val memoryTotalMB: Long = 0,
    val highTemperature: Boolean = false,   // crossed the safety threshold
    val thresholdC: Int = 60                // configured safety threshold
)