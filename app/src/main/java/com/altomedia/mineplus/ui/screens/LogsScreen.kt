package com.altomedia.mineplus.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.altomedia.mineplus.model.LogLevel
import com.altomedia.mineplus.ui.dashboard.DashboardViewModel
import com.altomedia.mineplus.ui.theme.MineDanger
import com.altomedia.mineplus.ui.theme.MineTextSecondary
import com.altomedia.mineplus.ui.theme.MineWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(vm: DashboardViewModel = hiltViewModel()) {
    val logs by vm.logs.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        item {
            Text(
                text = "Miner Log",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(8.dp)
            )
        }
        items(logs, key = { it.id }) { entry ->
            val color = when (entry.level) {
                LogLevel.ERROR -> MineDanger
                LogLevel.WARN -> MineWarning
                else -> MineTextSecondary
            }
            val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(entry.timestamp))
            Text(
                text = "[$time] ${entry.message}",
                color = color,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}