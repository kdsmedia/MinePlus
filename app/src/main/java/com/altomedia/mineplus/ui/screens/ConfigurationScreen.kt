package com.altomedia.mineplus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.altomedia.mineplus.model.MinerSettings
import com.altomedia.mineplus.navigation.Destinations
import com.altomedia.mineplus.ui.config.ConfigurationViewModel
import com.altomedia.mineplus.ui.theme.MineCard
import com.altomedia.mineplus.ui.theme.MinePrimary
import com.altomedia.mineplus.ui.theme.MineTextSecondary

/**
 * SETTINGS — grouped settings menu:
 *
 * Mining      : Algorithm, Server, Protocol, Port, Worker, Password, Intensity
 * Connection  : Auto Reconnect, Retry Delay
 * Device      : Temperature Limit, Battery Limit, Performance
 * Application : Notifications, Dark Mode, Start on Boot, About
 */
@Composable
fun ConfigurationScreen(
    vm: ConfigurationViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {}
) {
    val s by vm.settings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // ──────────────────────── Mining ─────────────────────────────────
        SectionHeader("Mining")
        SettingsRow {
            OutlinedTextField(
                value = s.algorithm,
                onValueChange = {},
                label = { Text("Algorithm") },
                readOnly = true,
                singleLine = true,
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
        SettingsRow {
            OutlinedTextField(
                value = s.host,
                onValueChange = { newHost -> vm.update { it.copy(host = newHost) } },
                label = { Text("Server") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Protocol
        SettingsLabel("Protocol")
        SettingsRow {
            ProtocolOption(
                label = "SSL",
                selected = s.useSsl,
                onSelect = { vm.update { it.copy(useSsl = true, port = 443) } }
            )
            ProtocolOption(
                label = "TCP",
                selected = !s.useSsl,
                onSelect = { vm.update { it.copy(useSsl = false, port = 9200) } }
            )
        }

        SettingsRow {
            OutlinedTextField(
                value = s.port.toString(),
                onValueChange = { newPort ->
                    vm.update { it.copy(port = newPort.toIntOrNull() ?: it.port) }
                },
                label = { Text("Port") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SettingsRow {
            OutlinedTextField(
                value = s.rigName,
                onValueChange = { newRig -> vm.update { it.copy(rigName = newRig) } },
                label = { Text("Worker") },
                placeholder = { Text("ANDROID01") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Password (masked by default)
        SettingsRow {
            var passwordVisible by rememberSaveable { mutableStateOf(false) }
            OutlinedTextField(
                value = s.password,
                onValueChange = { newPwd -> vm.update { it.copy(password = newPwd) } },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff
                            else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Sembunyikan password"
                            else "Tampilkan password",
                            tint = MineTextSecondary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Intensity
        SettingsLabel("Intensity")
        SettingsRow {
            MiningIntensitySection(
                intensity = s.miningIntensityPercent,
                enabled = s.intensityEnabled,
                onToggle = { on -> vm.update { it.copy(intensityEnabled = on) } },
                onSelect = { newIntensity ->
                    vm.update { it.copy(miningIntensityPercent = newIntensity.coerceIn(0, 100)) }
                }
            )
        }

        Spacer(Modifier.height(4.dp))

        // ─────────────────────── Connection ──────────────────────────────
        SectionHeader("Connection")
        SettingsToggleRow(
            label = "Auto Reconnect",
            checked = s.autoReconnect,
            onCheckedChange = { checked -> vm.update { it.copy(autoReconnect = checked) } }
        )
        SettingsLabel("Retry Delay")
        SettingsRow {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                MinerSettings.RECONNECT_INTERVALS.forEach { seconds ->
                    ProtocolOption(
                        label = "${seconds} sec",
                        selected = s.reconnectIntervalSec == seconds,
                        onSelect = { vm.update { it.copy(reconnectIntervalSec = seconds) } }
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // ──────────────────────── Device ─────────────────────────────────
        SectionHeader("Device")
        BatteryRow(
            label = "Temperature Limit",
            value = "${s.stopTempC}°C",
            onDecrease = { vm.update { it.copy(stopTempC = (s.stopTempC - 5).coerceAtLeast(40)) } },
            onIncrease = { vm.update { it.copy(stopTempC = (s.stopTempC + 5).coerceAtMost(95)) } }
        )
        BatteryRow(
            label = "Battery Limit",
            value = "${s.minBatteryLevel}%",
            onDecrease = { vm.update { it.copy(minBatteryLevel = (s.minBatteryLevel - 5).coerceAtLeast(0)) } },
            onIncrease = { vm.update { it.copy(minBatteryLevel = (s.minBatteryLevel + 5).coerceAtMost(100)) } }
        )
        SettingsToggleRow(
            label = "Performance",
            checked = s.intensityEnabled,
            onCheckedChange = { on -> vm.update { it.copy(intensityEnabled = on) } }
        )
        SettingsRow {
            OutlinedTextField(
                value = "${s.threads}",
                onValueChange = { newThreads ->
                    vm.update { it.copy(threads = newThreads.toIntOrNull()?.coerceAtLeast(1) ?: it.threads) }
                },
                label = { Text("Threads") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(4.dp))

        // ────────────────────── Application ──────────────────────────────
        SectionHeader("Application")
        SettingsToggleRow(
            label = "Notifications",
            checked = s.notificationsEnabled,
            onCheckedChange = { checked -> vm.update { it.copy(notificationsEnabled = checked) } }
        )
        SettingsToggleRow(
            label = "Dark Mode",
            checked = s.darkMode,
            onCheckedChange = { checked -> vm.update { it.copy(darkMode = checked) } }
        )
        SettingsToggleRow(
            label = "Start on Boot",
            checked = s.startOnBoot,
            onCheckedChange = { checked -> vm.update { it.copy(startOnBoot = checked) } }
        )
        // Extra auto-start toggles (kept functional, grouped under Application)
        SettingsToggleRow(
            label = "Start when app opens",
            checked = s.autoStartOnAppOpen,
            onCheckedChange = { checked -> vm.update { it.copy(autoStartOnAppOpen = checked) } }
        )
        SettingsToggleRow(
            label = "Start on charger connect",
            checked = s.autoStartOnCharger,
            onCheckedChange = { checked -> vm.update { it.copy(autoStartOnCharger = checked) } }
        )
        SettingsToggleRow(
            label = "Background Mining",
            checked = s.backgroundMining,
            onCheckedChange = { checked -> vm.update { it.copy(backgroundMining = checked) } }
        )
        SettingsNavRow(label = "About", onClick = { onNavigate(Destinations.ABOUT) })

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        color = MineTextSecondary,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsRow(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MineCard),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Text(
        text = text,
        color = MineTextSecondary,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MineCard),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MinePrimary,
                    checkedTrackColor = MinePrimary.copy(alpha = 0.4f)
                )
            )
        }
    }
}

@Composable
private fun SettingsNavRow(label: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MineCard),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MineTextSecondary
            )
        }
    }
}

@Composable
private fun ProtocolOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = MinePrimary)
        )
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        val trailing = if (label == "SSL") "stratum+ssl://x11.auto.nicehash.com:443"
        else if (label == "TCP") "stratum+tcp://x11.auto.nicehash.com:9200" else null
        if (selected && trailing != null) {
            Text(
                text = trailing,
                color = MineTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun BatteryRow(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MineCard),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            AdjustmentButton(text = "−") { onDecrease() }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MinePrimary,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            AdjustmentButton(text = "+") { onIncrease() }
        }
    }
}

@Composable
private fun MiningIntensitySection(
    intensity: Int,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Enable",
                style = MaterialTheme.typography.bodyMedium,
                color = MineTextSecondary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MinePrimary,
                    checkedTrackColor = MinePrimary.copy(alpha = 0.4f)
                )
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            IntensityRowItem(
                label = "Low",
                value = 30,
                intensity = intensity,
                enabled = enabled,
                onSelect = onSelect
            )
            IntensityRowItem(
                label = "Medium",
                value = 50,
                intensity = intensity,
                enabled = enabled,
                onSelect = onSelect
            )
            IntensityRowItem(
                label = "High",
                value = 90,
                intensity = intensity,
                enabled = enabled,
                onSelect = onSelect
            )
        }
        Spacer(Modifier.height(6.dp))
        IntensityBar(percent = intensity, enabled = enabled)
        Text(
            text = "Current: ${intensity}%",
            color = if (enabled) MinePrimary else MineTextSecondary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
        )
    }
}

@Composable
private fun IntensityRowItem(
    label: String,
    value: Int,
    intensity: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit
) {
    val selected = intensity == value
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = label,
            color = when {
                selected && enabled -> MinePrimary
                enabled -> androidx.compose.ui.graphics.Color(0xFFCFE3FF)
                else -> MineTextSecondary
            },
            fontWeight = if (selected && enabled) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(enabled = enabled) { onSelect(value) }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            repeat(4) { idx ->
                val idxOfPreset = when (value) { 30 -> 0; 50 -> 1; else -> 3 }
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            color = when {
                                !enabled -> MineTextSecondary.copy(alpha = 0.2f)
                                idx <= idxOfPreset -> MinePrimary
                                else -> MineTextSecondary.copy(alpha = 0.3f)
                            },
                            shape = RoundedCornerShape(50)
                        )
                )
                if (idx < 3) Spacer(Modifier.width(3.dp))
            }
        }
    }
}

@Composable
private fun IntensityBar(percent: Int, enabled: Boolean) {
    val barColor = if (enabled) MinePrimary.copy(alpha = 0.7f) else MineTextSecondary.copy(alpha = 0.2f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(MineTextSecondary.copy(alpha = 0.15f), RoundedCornerShape(50))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = percent / 100f)
                .height(6.dp)
                .background(barColor, RoundedCornerShape(50))
        )
    }
}

@Composable
private fun AdjustmentButton(text: String, onClick: () -> Unit) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.height(36.dp)
    ) {
        Text(text, fontSize = 16.sp)
    }
}