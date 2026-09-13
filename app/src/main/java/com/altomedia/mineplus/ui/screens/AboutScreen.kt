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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altomedia.mineplus.navigation.Destinations
import com.altomedia.mineplus.ui.theme.MineCard
import com.altomedia.mineplus.ui.theme.MinePrimary
import com.altomedia.mineplus.ui.theme.MineTextSecondary

/**
 * ABOUT page: brand, version, developer, product tagline and legal links
 * (Open Source Licenses, Privacy, Terms).
 */
@Composable
fun AboutScreen(
    onNavigate: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = MinePrimary.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.large
                )
        ) {
            Text(
                text = "MP",
                color = MinePrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = "MinePlus",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Version 1.0.0",
            fontFamily = FontFamily.Monospace,
            color = MineTextSecondary,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "X11 Mining Controller",
            color = MineTextSecondary,
            fontSize = 15.sp
        )
        Text(
            text = "for NiceHash Stratum",
            color = MineTextSecondary,
            fontSize = 15.sp
        )

        Spacer(Modifier.height(24.dp))

        LegalLinkRow(label = "Open Source Licenses") {
            onNavigate(Destinations.LICENSES)
        }
        LegalLinkRow(label = "Privacy") {
            onNavigate(Destinations.PRIVACY)
        }
        LegalLinkRow(label = "Terms") {
            onNavigate(Destinations.TERMS)
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = "© 2026 ALTOMEDIA",
            color = MineTextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LegalLinkRow(label: String, onClick: () -> Unit) {
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

/** Shared scaffold used by the legal sub-pages (licenses/privacy/terms). */
@Composable
internal fun LegalPage(
    title: String,
    sections: List<Pair<String, String>>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        sections.forEach { (heading, body) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = heading,
                    style = MaterialTheme.typography.titleMedium,
                    color = MinePrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MineTextSecondary
                )
            }
        }
    }
}