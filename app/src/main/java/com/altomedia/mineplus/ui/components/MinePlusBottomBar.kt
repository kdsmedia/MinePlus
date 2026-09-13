package com.altomedia.mineplus.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.altomedia.mineplus.navigation.Destinations
import com.altomedia.mineplus.ui.theme.MinePrimary
import com.altomedia.mineplus.ui.theme.MineSurface

private data class BottomBarItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val items = listOf(
    BottomBarItem(Destinations.DASHBOARD, "Dashboard", Icons.Filled.Home),
    BottomBarItem(Destinations.MINING, "Mining", Icons.Filled.Memory),
    BottomBarItem(Destinations.STATISTICS, "Stats", Icons.Filled.Star),
    BottomBarItem(Destinations.LOGS, "Logs", Icons.Filled.Info),
    BottomBarItem(Destinations.DEVICE, "Temp", Icons.Filled.Thermostat),
    BottomBarItem(Destinations.CONFIGURATION, "Config", Icons.Filled.Settings)
)

@Composable
fun MinePlusBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    // Hide the navigation bar while the splash screen is shown.
    if (currentRoute == Destinations.SPLASH) return

    NavigationBar(containerColor = MineSurface) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MinePrimary,
                    selectedTextColor = MinePrimary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    indicatorColor = MinePrimary.copy(alpha = 0.12f)
                )
            )
        }
    }
}