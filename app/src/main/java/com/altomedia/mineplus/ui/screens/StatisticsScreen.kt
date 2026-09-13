package com.altomedia.mineplus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.altomedia.mineplus.ui.dashboard.DashboardViewModel
import com.altomedia.mineplus.ui.theme.MineTextSecondary

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

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCell("Hashrate", formatHashrate(state.hashrate), Modifier.weight(1f))
            StatCell("Difficulty", "%.4f".format(state.difficulty), Modifier.weight(1f))
        }

        StatCell("Accepted Shares", state.acceptedShares.toString(), Modifier.fillMaxWidth())
        StatCell("Rejected Shares", state.rejectedShares.toString(), Modifier.fillMaxWidth())
        StatCell("Total Shares", state.totalShares.toString(), Modifier.fillMaxWidth())
        StatCell("Uptime", formatUptime(state.uptimeSeconds), Modifier.fillMaxWidth())
        StatCell("Pool", state.poolEndpoint.ifBlank { "n/a" }, Modifier.fillMaxWidth())
        StatCell("Connected", state.connected.toString(), Modifier.fillMaxWidth())
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label.uppercase(),
                color = MineTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
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