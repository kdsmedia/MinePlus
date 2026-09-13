package com.altomedia.mineplus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.altomedia.mineplus.model.LogLevel
import com.altomedia.mineplus.ui.dashboard.DashboardViewModel
import com.altomedia.mineplus.ui.theme.MineDanger
import com.altomedia.mineplus.ui.theme.MinePrimary
import com.altomedia.mineplus.ui.theme.MineTextSecondary
import com.altomedia.mineplus.ui.theme.MineWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(vm: DashboardViewModel = hiltViewModel()) {
    val logs by vm.logs.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var autoScroll by remember { mutableStateOf(true) }

    // Auto-scroll to the newest entry when new logs arrive.
    LaunchedEffect(logs.size) {
        if (autoScroll && logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = "MINER LOG",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        // Toolbar: Clear / Copy / Auto Scroll toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { vm.clearMinerLogs() },
                enabled = logs.isNotEmpty()
            ) {
                Text("[ Clear ]")
            }
            OutlinedButton(
                onClick = {
                    val text = buildString {
                        logs.forEach { entry ->
                            val time = SimpleDateFormat("HH:mm:ss", Locale.US)
                                .format(Date(entry.timestamp))
                            appendLine("$time  ${entry.message}")
                        }
                    }
                    clipboard.setText(AnnotatedString(text))
                    android.widget.Toast.makeText(
                        context, "Logs copied", android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                enabled = logs.isNotEmpty()
            ) {
                Text("[ Copy ]")
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "Auto Scroll",
                color = MineTextSecondary,
                fontSize = 12.sp
            )
            Switch(
                checked = autoScroll,
                onCheckedChange = { autoScroll = it }
            )
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(logs, key = { it.id }) { entry ->
                val color = when (entry.level) {
                    LogLevel.ERROR -> MineDanger
                    LogLevel.WARN -> MineWarning
                    LogLevel.INFO -> MinePrimary
                    LogLevel.DEBUG -> MineTextSecondary
                }
                val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(entry.timestamp))
                Text(
                    text = "$time  ${entry.message}",
                    color = color,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
                )
            }
        }
    }
}