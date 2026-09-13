package com.altomedia.mineplus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.altomedia.mineplus.ui.theme.MineTextSecondary

/**
 * Dedicated mining screen shown while the miner is running (see HALAMAN
 * MINING). Centered status, hashrate, pool info, share stats and a big
 * STOP MINING button.
 */
@Composable
fun MiningScreen(
    vm: DashboardViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val state by vm.minerState.collectAsStateWithLifecycle()

    // Leave the mining screen automatically only when mining transitions
    // from active to stopped/error (not when opened while already idle).
    var previousWasMining by remember { mutableStateOf(isActive(state.status)) }
    LaunchedEffect(state.status) {
        val wasMining = previousWasMining
        previousWasMining = isActive(state.status)
        if (wasMining && !isActive(state.status)) {
            onBack()
        }
    }

    val displayHashrate = state.hashrate
    val poolDesc = parsePool(state.poolEndpoint)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Top bar: ← Mining ─────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.material3.TextButton(onClick = onBack) {
                Text("←", color = MineTextSecondary, fontSize = 20.sp)
                Text(
                    " Mining",
                    color = MineTextSecondary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Pulsing dot + MINING ──────────────────────────────────────────
        MiningIndicator(enabled = true)

        // ── Hashrate ──────────────────────────────────────────────────────
        Text(
            text = formatHashrate(displayHashrate),
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = MinePrimary,
            fontFamily = FontFamily.SansSerif
        )

        // ── Pool info: X11 / NiceHash / SSL :443 ──────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Spacer(Modifier.height(6.dp))
            InfoLine("Algorithm", "X11")
            InfoLine(
                "Pool",
                if (poolDesc.host.contains("nicehash", ignoreCase = true)) "NiceHash"
                else parseHost(poolDesc.host)
            )
            InfoLine(
                "Connection",
                buildString {
                    append(if (poolDesc.secure) "SSL" else "TCP")
                    poolDesc.port?.let { append(" :$it") }
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── Accepted / Rejected ───────────────────────────────────────────
        ShareStats(accepted = state.acceptedShares, rejected = state.rejectedShares)

        // ── Uptime ────────────────────────────────────────────────────────
        InfoLine("Uptime", formatUptime(state.uptimeSeconds))

        Spacer(Modifier.weight(1f))

        // ── STOP MINING ───────────────────────────────────────────────────
        Button(
            onClick = vm::stopMining,
            colors = ButtonDefaults.buttonColors(containerColor = MineError),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                "STOP MINING",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}

private fun isActive(status: MinerStatus): Boolean =
    status == MinerStatus.MINING ||
        status == MinerStatus.CONNECTED ||
        status == MinerStatus.CONNECTING

@Composable
private fun MiningIndicator(enabled: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = if (enabled) MinePrimary.copy(alpha = 0.18f) else MineTextSecondary.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        color = if (enabled) MinePrimary else MineTextSecondary,
                        shape = CircleShape
                    )
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (enabled) "MINING" else "STOPPED",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) MinePrimary else MineTextSecondary
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MineTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ShareStats(accepted: Long, rejected: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatPill("Accepted", accepted.toString(), MinePrimary)
        StatPill("Rejected", rejected.toString(), MineDanger)
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .weight(1f)
            .background(MineCard, MaterialTheme.shapes.medium)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = MineTextSecondary,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            color = color,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

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

private data class PoolParts(val host: String = "", val port: Int? = null, val secure: Boolean = false)

private fun parsePool(endpoint: String): PoolParts {
    if (endpoint.isBlank()) return PoolParts()
    val secure = endpoint.startsWith("stratum+ssl")
    val withoutScheme = endpoint.substringAfter("://")
    val host = withoutScheme.substringBefore(":")
    val port = withoutScheme.substringAfter(":", "").toIntOrNull()
    return PoolParts(host = host, port = port, secure = secure)
}

private fun parseHost(raw: String): String =
    if (raw.isEmpty()) "-" else raw