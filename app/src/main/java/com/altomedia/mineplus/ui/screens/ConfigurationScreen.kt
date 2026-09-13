package com.altomedia.mineplus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.altomedia.mineplus.ui.config.ConfigurationViewModel
import com.altomedia.mineplus.ui.theme.MineCard
import com.altomedia.mineplus.ui.theme.MinePrimary
import com.altomedia.mineplus.ui.theme.MineTextSecondary

@Composable
fun ConfigurationScreen(vm: ConfigurationViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "MINING CONFIGURATION",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Algorithm (fixed to X11)
        OutlinedTextField(
            value = s.algorithm,
            onValueChange = { vm.update { it.copy(algorithm = it.algorithm) } },
            label = { Text("Algorithm") },
            readOnly = true,
            trailingIcon = { Text("X11", color = MineTextSecondary) },
            singleLine = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )

        // Server host
        OutlinedTextField(
            value = s.host,
            onValueChange = { newHost -> vm.update { it.copy(host = newHost) } },
            label = { Text("Server") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Protocol: SSL / TCP radio
        SettingsLabel("Protocol")
        Card(
            colors = CardDefaults.cardColors(containerColor = MineCard),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                ProtocolOption(
                    label = "SSL",
                    selected = s.useSsl,
                    onSelect = {
                        vm.update { it.copy(useSsl = true, port = 443) }
                    }
                )
                ProtocolOption(
                    label = "TCP",
                    selected = !s.useSsl,
                    onSelect = {
                        vm.update { it.copy(useSsl = false, port = 9200) }
                    }
                )
            }
        }

        // Endpoint preview (auto)
        OutlinedTextField(
            value = s.endpoint,
            onValueChange = {},
            label = { Text("Endpoint (otomatis)") },
            readOnly = true,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Port
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

        // Username
        OutlinedTextField(
            value = s.walletAddress,
            onValueChange = { newUser -> vm.update { it.copy(walletAddress = newUser) } },
            label = { Text("Username") },
            placeholder = { Text("YOUR_NICEHASH_USERNAME") },
            supportingText = {
                Text("Username NiceHash / BTC address", color = MineTextSecondary, fontSize = 11.sp)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Worker name
        OutlinedTextField(
            value = s.rigName,
            onValueChange = { newRig -> vm.update { it.copy(rigName = newRig) } },
            label = { Text("Worker") },
            placeholder = { Text("ANDROID01") },
            supportingText = {
                Text(
                    "Contoh: ANDROID01, PHONE01, X11-01, MINER01",
                    color = MineTextSecondary,
                    fontSize = 11.sp
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Combined worker config preview
        OutlinedTextField(
            value = s.login,
            onValueChange = {},
            label = { Text("Konfigurasi") },
            readOnly = true,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Password (masked by default)
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

        // Toggles
        SettingsToggle(
            label = "Auto Reconnect",
            checked = s.autoReconnect,
            onCheckedChange = { checked -> vm.update { it.copy(autoReconnect = checked) } }
        )

        // Reconnect interval selector
        SettingsLabel("Reconnect Interval")
        Card(
            colors = CardDefaults.cardColors(containerColor = MineCard),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                MinerSettings.RECONNECT_INTERVALS.forEach { seconds ->
                    ProtocolOption(
                        label = "$seconds sec",
                        selected = s.reconnectIntervalSec == seconds,
                        onSelect = { vm.update { it.copy(reconnectIntervalSec = seconds) } },
                        trailingText = null
                    )
                }
            }
        }

        // Maximum retries selector
        OutlinedTextField(
            value = s.maxReconnects.toString(),
            onValueChange = { newMax ->
                vm.update { it.copy(maxReconnects = newMax.toIntOrNull() ?: it.maxReconnects) }
            },
            label = { Text("Max Reconnect Attempts") },
            supportingText = {
                Text("Maksimum retry otomatis", color = MineTextSecondary, fontSize = 11.sp)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        SettingsToggle(
            label = "Start on Boot",
            checked = s.startOnBoot,
            onCheckedChange = { checked -> vm.update { it.copy(startOnBoot = checked) } }
        )
        SettingsToggle(
            label = "Background Mining",
            checked = s.backgroundMining,
            onCheckedChange = { checked -> vm.update { it.copy(backgroundMining = checked) } }
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = vm::persist,
            colors = ButtonDefaults.buttonColors(containerColor = MinePrimary),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "SAVE MINING CONFIGURATION",
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color(0xFF06251B),
                modifier = Modifier.padding(4.dp)
            )
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
private fun ProtocolOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    trailingText: String? = null
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
        val trailing = trailingText ?: if (label == "SSL") "stratum+ssl://x11.auto.nicehash.com:443"
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
private fun SettingsToggle(
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

