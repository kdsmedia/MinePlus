package com.altomedia.mineplus.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altomedia.mineplus.ui.theme.MineBackground
import com.altomedia.mineplus.ui.theme.MinePrimary
import com.altomedia.mineplus.ui.theme.MineSecondary
import com.altomedia.mineplus.ui.theme.MineTextSecondary
import kotlinx.coroutines.delay

/**
 * Branded splash: a simple pulsing "X" mark with the product name and a
 * three-dot progress indicator. Shows for roughly 1.5s, then lands on the
 * dashboard.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val enter by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 700),
        label = "splashEnter"
    )

    val infinite = rememberInfiniteTransition(label = "splashPulse")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoPulse"
    )

    LaunchedEffect(Unit) {
        delay(1500)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MineBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Stylised X mark.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(pulse)
                    .alpha(enter)
                    .size(96.dp)
                    .background(
                        color = MinePrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Text(
                    text = "X",
                    color = MinePrimary,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "X11 MINER",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(enter)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "NiceHash Stratum",
                color = MineSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.alpha(enter)
            )

            Spacer(Modifier.height(48.dp))

            ThreeDotProgress()
        }
    }
}

@Composable
private fun ThreeDotProgress() {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 3) {
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, delayMillis = i * 150),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$i"
            )
            Spacer(
                Modifier
                    .size(8.dp)
                    .background(MineTextSecondary.copy(alpha = alpha), CircleShape)
            )
        }
    }
}