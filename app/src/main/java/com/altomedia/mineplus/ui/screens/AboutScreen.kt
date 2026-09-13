package com.altomedia.mineplus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.altomedia.mineplus.ui.theme.MineTextSecondary

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "MinePlus",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "X11 ASIC/CPU miner for NiceHash",
            color = MineTextSecondary
        )

        Spacer(Modifier.size(8.dp))

        Text(
            text = "Version: 1.0.0",
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Algorithm: X11 (Dash lineage)",
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Developer: ALTOMEDIA",
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Package: com.altomedia.mineplus",
            fontFamily = FontFamily.Monospace
        )

        Spacer(Modifier.size(8.dp))

        Text(
            text = "Mining happens natively via the Android NDK (ARM64 / ARMv7). " +
                "The X11 engine is based on the public-domain sphlib implementation.",
            color = MineTextSecondary
        )
    }
}