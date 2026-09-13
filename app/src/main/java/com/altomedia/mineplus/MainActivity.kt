package com.altomedia.mineplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.altomedia.mineplus.navigation.MinePlusApp
import com.altomedia.mineplus.ui.theme.MinePlusTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MinePlusTheme {
                Surface {
                    MinePlusApp()
                }
            }
        }
    }
}