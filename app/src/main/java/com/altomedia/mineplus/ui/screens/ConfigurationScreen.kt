package com.altomedia.mineplus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.altomedia.mineplus.ui.config.ConfigurationViewModel

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
            text = "Pool Configuration",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = s.host,
            onValueChange = { newHost ->
                vm.update { it.copy(host = newHost) }
            },
            label = { Text("Stratum Host") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
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
        OutlinedTextField(
            value = s.walletAddress,
            onValueChange = { newWallet ->
                vm.update { it.copy(walletAddress = newWallet) }
            },
            label = { Text("Wallet / BTC Address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = s.rigName,
            onValueChange = { newRig ->
                vm.update { it.copy(rigName = newRig) }
            },
            label = { Text("Worker Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // SSL toggle handled below.
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("Use SSL")
            androidx.compose.foundation.layout.Spacer(Modifier.padding(4.dp))
            Switch(
                checked = s.useSsl,
                onCheckedChange = { vm.update { it.copy(useSsl = it.useSsl) } }
            )
        }

        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("Start on boot")
            androidx.compose.foundation.layout.Spacer(Modifier.padding(4.dp))
            Switch(
                checked = s.startOnBoot,
                onCheckedChange = { vm.update { it.copy(startOnBoot = it.startOnBoot) } }
            )
        }

        Button(
            onClick = vm::persist,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SAVE")
        }
    }
}