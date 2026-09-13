package com.altomedia.mineplus.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.altomedia.mineplus.ui.dashboard.DashboardViewModel
import com.altomedia.mineplus.ui.theme.MineAccent
import com.altomedia.mineplus.ui.theme.MineCard
import com.altomedia.mineplus.ui.theme.MinePrimary
import com.altomedia.mineplus.ui.theme.MineTextSecondary
import kotlin.math.max
import kotlin.math.roundToLong

@Composable
fun StatisticsScreen(vm: DashboardViewModel = hiltViewModel()) {
    val state by vm.minerState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Statistics",
            style = MaterialTheme.typography.headlineMedium
        )

        // Current + Average hash rate
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Current Hashrate", formatHashrate(state.hashrate), Modifier.weight(1f))
            StatCard("Average Hashrate", formatHashrate(state.averageHashrate), Modifier.weight(1f))
        }

        // Accepted / Rejected / Reject rate
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Accepted", "%,d".format(state.acceptedShares), Modifier.weight(1f))
            StatCard("Rejected", "%,d".format(state.rejectedShares), Modifier.weight(1f))
            StatCard("Reject Rate", String.format("%.2f%%", state.rejectRatePercent), Modifier.weight(1f))
        }

        // Uptime / Shares per hour
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Uptime", formatUptime(state.uptimeSeconds), Modifier.weight(1f))
            StatCard("Shares / Hour", "%,.0f".format(state.sharesPerHour), Modifier.weight(1f))
        }

        // Last share
        StatCard("Last Share", formatLastShare(state.lastShareSecondsAgo), Modifier.fillMaxWidth())

        // Hashrate chart
        HashrateChartCard(state.hashrateHistory, state.hashrate)
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MineCard)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label.uppercase(),
                color = MineTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun HashrateChartCard(history: List<Double>, current: Double, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MineCard)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HASHRATE",
                        color = MineTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = formatHashrate(current),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MinePrimary
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            HashrateChart(
                history = history,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Time →",
                color = MineTextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun HashrateChart(history: List<Double>, modifier: Modifier = Modifier) {
    val maxRate = max(history.maxOrNull() ?: 0.0, 1.0)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padTop = 10f
        val padBottom = 8f
        val plotH = h - padTop - padBottom

        // Horizontal gridlines
        listOf(0f, 0.33f, 0.66f, 1f).forEach { f ->
            val y = padTop + plotH * f
            drawLine(
                color = Color(0xFF232323),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
        }

        if (history.size < 2) return@Canvas

        val stepX = w / (history.size - 1)
        val path = Path()
        history.forEachIndexed { i, rate ->
            val x = i * stepX
            val y = padTop + plotH * (1f - (rate / maxRate).toFloat()).coerceIn(0f, 1f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = MineAccent,
            style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Area fill under the curve
        val fillPath = Path().apply {
            addPath(path)
            lineTo(w, padTop + plotH)
            lineTo(0f, padTop + plotH)
            close()
        }
        drawPath(
            path = fillPath,
            color = MineAccent.copy(alpha = 0.12f)
        )
    }
}

@Composable
private fun formatHashrate(h: Double): String = when {
    h >= 1_000_000 -> "%.2f MH/s".format(h / 1_000_000)
    h >= 1_000 -> "%.2f KH/s".format(h / 1_000)
    else -> "%.1f H/s".format(h)
}

@Composable
private fun formatUptime(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

@Composable
private fun formatLastShare(secondsAgo: Long): String = when {
    secondsAgo == Long.MAX_VALUE -> "No shares yet"
    secondsAgo < 2 -> "just now"
    secondsAgo < 60 -> "$secondsAgo seconds ago"
    secondsAgo < 3600 -> "${(secondsAgo / 60).roundToLong()} minutes ago"
    else -> "${(secondsAgo / 3600).roundToLong()} hours ago"
}