package com.altomedia.mineplus.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.altomedia.mineplus.ui.components.MinePlusBottomBar
import com.altomedia.mineplus.ui.screens.AboutScreen
import com.altomedia.mineplus.ui.screens.ConfigurationScreen
import com.altomedia.mineplus.ui.screens.DashboardScreen
import com.altomedia.mineplus.ui.screens.LogsScreen
import com.altomedia.mineplus.ui.screens.StatisticsScreen

object Destinations {
    const val SPLASH = "splash"
    const val DASHBOARD = "dashboard"
    const val MINING = "mining"
    const val CONFIGURATION = "configuration"
    const val STATISTICS = "statistics"
    const val LOGS = "logs"
    const val ABOUT = "about"
}

@Composable
fun MinePlusApp() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            MinePlusBottomBar(
                currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Destinations.DASHBOARD) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.SPLASH,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destinations.SPLASH) {
                SplashScreen(
                    onFinished = {
                        navController.navigate(Destinations.DASHBOARD) {
                            popUpTo(Destinations.SPLASH) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Destinations.DASHBOARD) { DashboardScreen() }
            composable(Destinations.MINING) { DashboardScreen() }
            composable(Destinations.CONFIGURATION) { ConfigurationScreen() }
            composable(Destinations.STATISTICS) { StatisticsScreen() }
            composable(Destinations.LOGS) { LogsScreen() }
            composable(Destinations.ABOUT) { AboutScreen() }
        }
    }
}