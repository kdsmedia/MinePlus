package com.altomedia.mineplus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.altomedia.mineplus.model.DeviceStats
import com.altomedia.mineplus.ui.device.DeviceViewModel
import com.altomedia.mineplus.ui.theme.MineCard
import com.altomedia.mineplus.ui.theme.MineDanger
import com.altomedia.mineplus.ui.theme.MinePrimary
import com.altomedia.mineplus.ui.theme.MineTextSecondary
import com.altomedia.mineplus.ui.theme.MineWarning

@Composable
fun DeviceScreen(vm: DeviceViewModel = hiltViewModel()) {
    val stats by vm.stats.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "TEMPERATUR",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Device health & sensors",
            style = MaterialTheme.typography.bodyMedium,
            color = MineTextSecondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (stats.highTemperature) {
            HighTempAlert(
                threshold = stats.thresholdC,
                minerThrottled = true
            )
            Spacer(Modifier.height(8.dp))
        }

        DeviceStatusCard(stats = stats)
    }
}

@Composable
private fun HighTempAlert(threshold: Int, minerThrottled: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MineDanger.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .border(1.dp, MineDanger.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "⚠ High Temperature",
            color = MineDanger,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Text(
            text = "Device temperature is above the configured safety threshold ($threshold°C).",
            color = MineTextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = if (minerThrottled) "Mining has been reduced/stopped." else "Mining may be reduced.",
            color = MineWarning,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun DeviceStatusCard(stats: DeviceStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MineCard, RoundedCornerShape(14.dp))
            .border(1.dp, MineCard, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "DEVICE",
            style = MaterialTheme.typography.titleMedium,
            color = MinePrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(10.dp))

        MetricRow(
            label = "CPU Temperature",
            value = stats.cpuTemperatureC?.let { "${it.round1()}°C" } ?: "N/A",
            valueColor = when {
                stats.cpuTemperatureC == null -> MineTextSecondary
                stats.highTemperature -> MineDanger
                stats.cpuTemperatureC >= 50 -> MineWarning
                else -> null
            }
        )
        HorizontalDivider(color = MineTextSecondary.copy(alpha = 0.15f))
        MetricRow(
            label = "Battery",
            value = if (stats.batteryPercent >= 0) "${stats.batteryPercent}%" else "N/A",
            valueColor = if (stats.batteryPercent in 0..15) MineDanger else null
        )
        HorizontalDivider(color = MineTextSecondary.copy(alpha = 0.15f))
        MetricRow(
            label = "CPU Usage",
            value = if (stats.cpuUsagePercent >= 0) "${stats.cpuUsagePercent.round1()}%" else "N/A",
            valueColor = if (stats.cpuUsagePercent >= 90) MineDanger else null
        )
        HorizontalDivider(color = MineTextSecondary.copy(alpha = 0.15f))
        MetricRow(
            label = "Memory",
            value = formatMemory(stats.memoryUsedMB, stats.memoryTotalMB)
        )
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = label,
            color = MineTextSecondary,
            fontSize = 14.sp
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
    }
}

private fun Float.round1(): String = String.format(java.util.Locale.US, "%.1f", this)

private fun formatMemory(usedMB: Long, totalMB: Long): String {
    if (totalMB <= 0) return "N/A"
    return "${gb(usedMB)} GB / ${gb(totalMB)} GB"
}

private fun gb(mb: Long): String = String.format(java.util.Locale.US, "%.1f", mb / 1024.0)