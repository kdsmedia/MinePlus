package com.altomedia.mineplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.altomedia.mineplus.data.SettingsRepository
import com.altomedia.mineplus.miner.MinerManager
import com.altomedia.mineplus.navigation.MinePlusApp
import com.altomedia.mineplus.ui.theme.MinePlusTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var manager: MinerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start mining when the app opens (when enabled in AUTO START).
        // Gated by the pre-start validation: never start unless the engine
        // is actually available.
        lifecycleScope.launch(Dispatchers.IO) {
            val settings = settingsRepository.current()
            if (settings.autoStartOnAppOpen) {
                manager.start()
            }
        }

        setContent {
            var darkMode by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                settingsRepository.settings.collect { darkMode = it.darkMode }
            }
            MinePlusTheme(darkTheme = darkMode) {
                Surface {
                    MinePlusApp()
                }
            }
        }
    }
}