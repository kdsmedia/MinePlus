package com.altomedia.mineplus.ui.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.mineplus.model.DeviceStats
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.FileReader
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _stats = MutableStateFlow(DeviceStats())
    val stats: StateFlow<DeviceStats> = _stats.asStateFlow()

    private val _miningThermalThrottled = MutableStateFlow(false)
    val miningThermalThrottled: StateFlow<Boolean> = _miningThermalThrottled.asStateFlow()

    private var prevCpuTotal = -1L
    private var prevCpuIdle = -1L

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                _stats.value = collect()
                updateThrottle()
                delay(2000)
            }
        }
    }

    private fun collect(): DeviceStats {
        val threshold = 60
        val temp = readCpuTemperature()
        val hot = temp?.let { it >= threshold } ?: false
        return DeviceStats(
            cpuTemperatureC = temp,
            batteryPercent = readBattery(),
            cpuUsagePercent = readCpuUsage(),
            memoryUsedMB = readMemory().first,
            memoryTotalMB = readMemory().second,
            highTemperature = hot,
            thresholdC = threshold
        )
    }

    /** Emits once when the threshold is first crossed. */
    private fun updateThrottle() {
        val hot = _stats.value.highTemperature
        if (hot && !_miningThermalThrottled.value) {
            _miningThermalThrottled.value = true
        } else if (!hot && _miningThermalThrottled.value) {
            _miningThermalThrottled.value = false
        }
    }

    /**
     * Best-effort SoC temperature via sysfs thermal zones. Returns null when
     * the device exposes no readable zone (common on emulators).
     */
    private fun readCpuTemperature(): Float? {
        var best: Float? = null
        var bestScore = -1
        for (i in 0 until 12) {
            val value = readSysFsFloat("/sys/class/thermal/thermal_zone$i/temp") ?: continue
            val score = when {
                value in 250.0f..950.0f -> 3
                value in 120.0f..1200.0f -> 2
                else -> 1
            }
            if (score > bestScore) {
                bestScore = score
                best = value
            }
        }
        val fallback = readSysFsFloat("/sys/devices/system/cpu/cpu0/cpufreq/cooling_device/temp")
        if (fallback != null) {
            val score = if (fallback in 250.0f..950.0f) 3 else 1
            if (score > bestScore) {
                bestScore = score
                best = fallback
            }
        }
        if (best == null) return null
        // sysfs is usually millidegree; values that look like degrees are kept.
        return if (best > 120f && best < 150_000f) best / 1000f else best
    }

    private fun readSysFsFloat(path: String): Float? = try {
        BufferedReader(FileReader(path)).use { it.readLine()?.trim()?.toFloatOrNull() }
    } catch (_: Exception) {
        null
    }

    private fun readBattery(): Int {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return -1
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return -1
        return (level * 100 / scale).coerceIn(0, 100)
    }

    /** System-wide CPU busy percentage from /proc/stat deltas. */
    private fun readCpuUsage(): Float {
        var total = 0L
        var idle = 0L
        try {
            BufferedReader(FileReader("/proc/stat")).use { r ->
                val line = r.readLine() ?: return -1f
                if (!line.startsWith("cpu ")) return -1f
                val parts = line.split("\\s+".toRegex()).drop(1).mapNotNull { it.toLongOrNull() }
                if (parts.size < 4) return -1f
                idle = parts[3] + parts.getOrElse(4) { 0L } // idle + iowait
                total = parts.sum()
            }
        } catch (_: Exception) {
            return -1f
        }
        if (prevCpuTotal < 0) {
            prevCpuTotal = total
            prevCpuIdle = idle
            return 0f
        }
        val dTotal = total - prevCpuTotal
        val dIdle = idle - prevCpuIdle
        prevCpuTotal = total
        prevCpuIdle = idle
        if (dTotal <= 0) return 0f
        val busy = (dTotal - dIdle).toFloat() / dTotal
        return (busy * 100).coerceIn(0f, 100f)
    }

    private fun readMemory(): Pair<Long, Long> {
        var totalKb = 0L
        var availableKb = 0L
        try {
            BufferedReader(FileReader("/proc/meminfo")).use { r ->
                r.forEachLine { line ->
                    val parts = line.split("\\s+".toRegex())
                    when {
                        parts.firstOrNull() == "MemTotal:" -> totalKb = parts.getOrNull(1)?.toLongOrNull() ?: 0
                        parts.firstOrNull() == "MemAvailable:" -> availableKb = parts.getOrNull(1)?.toLongOrNull() ?: 0
                    }
                }
            }
        } catch (_: Exception) {
        }
        if (totalKb <= 0) return 0L to 0L
        val usedKb = totalKb - availableKb
        return (usedKb * 1024) to (totalKb * 1024)
    }
}