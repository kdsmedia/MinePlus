package com.altomedia.mineplus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.altomedia.mineplus.model.MinerStatus
import com.altomedia.mineplus.ui.dashboard.DashboardViewModel
import com.altomedia.mineplus.ui.theme.MineCard
import com.altomedia.mineplus.ui.theme.MineDanger
import com.altomedia.mineplus.ui.theme.MineError
import com.altomedia.mineplus.ui.theme.MinePrimary
import com.altomedia.mineplus.ui.theme.MineSuccess
import com.altomedia.mineplus.ui.theme.MineTextSecondary
import com.altomedia.mineplus.ui.theme.MineWarning

@Composable
fun DashboardScreen(vm: DashboardViewModel = hiltViewModel()) {
    val state by vm.minerState.collectAsStateWithLifecycle()

    val mining = state.status == MinerStatus.MINING ||
        state.status == MinerStatus.CONNECTED ||
        state.status == MinerStatus.CONNECTING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "X11 Miner",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = MineTextSecondary
            )
        }

        // Active status + pool
        StatusCard(status = state.status, mining = mining)
        if (mining) {
            Text(
                text = "Connected to NiceHash",
                color = MineTextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // Hashrate - hero card
        HashrateCard(
            hashrate = formatHashrate(state.hashrate),
            delta = "+2.4%",
            active = mining
        )

        // Accepted / Rejected
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Accepted", state.acceptedShares.toString(), Modifier.weight(1f))
            StatCard("Rejected", state.rejectedShares.toString(), Modifier.weight(1f))
        }

        // Uptime
        StatCard("Uptime", formatUptime(state.uptimeSeconds), Modifier.fillMaxWidth())

        Spacer(Modifier.height(4.dp))

        if (mining) {
            Button(
                onClick = vm::stopMining,
                colors = ButtonDefaults.buttonColors(containerColor = MineError),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("STOP MINING", fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
            }
        } else {
            Button(
                onClick = vm::startMining,
                colors = ButtonDefaults.buttonColors(containerColor = MinePrimary),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "START MINING",
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color(0xFF06251B),
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        state.lastError?.let { err ->
            Text(
                text = err,
                color = MineDanger,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun HashrateCard(hashrate: String, delta: String, active: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MineCard),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "HASHRATE",
                color = MineTextSecondary,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = hashrate,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = if (active) MinePrimary else MineText
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "▲ $delta",
                color = if (active) MinePrimary else MineTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun StatusCard(status: MinerStatus, mining: Boolean) {
    val (color, label) = when (status) {
        MinerStatus.STOPPED -> MineTextSecondary to "DISCONNECTED"
        MinerStatus.CONNECTING -> MineWarning to "CONNECTING…"
        MinerStatus.CONNECTED -> MineWarning to "CONNECTED"
        MinerStatus.MINING -> MineSuccess to "MINING ACTIVE"
        MinerStatus.ERROR -> MineDanger to "CONNECTION ERROR"
    }
    Surface(
        color = MineCard,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 2.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                color = color,
                shape = CircleShape,
                modifier = Modifier.size(12.dp)
            ) {}
            Spacer(Modifier.size(10.dp))
            Text(
                text = label,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(Modifier.weight(1f))
            if (mining) {
                Text(
                    text = "NiceHash X11 · SSL :443",
                    color = MineTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MineCard),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label.uppercase(),
                color = MineTextSecondary,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif
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