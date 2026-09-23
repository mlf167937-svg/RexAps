// /data/data/com.termux/files/home/RexAps/app/src/main/java/com/rexaps/ui/RexThemeLoading.kt
package com.rexaps.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Lightweight overlay shown briefly while a new theme is being applied,
 * instead of animating every color channel of the entire UI live.
 * Auto-dismisses after [minDurationMs] via [onFinished].
 */
@Composable
fun RexThemeLoadingOverlay(
    label: String = "Menerapkan tema",
    minDurationMs: Long = 420L,
    onFinished: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        delay(minDurationMs)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceVariant,
            shadowElevation = 24.dp,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                RexSpinner(size = 40.dp)

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Premium full-screen loader shown while a sub-app (RexFox, RexPanel, RexChat)
 * is opening. Minimum duration prevents a jarring flash on fast opens.
 */
@Composable
fun RexAppLoader(
    appName: String,
    minDurationMs: Long = 500L,
    onFinished: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(appName) {
        delay(minDurationMs)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = CircleShape,
                        ambientColor = colors.primary.copy(alpha = 0.4f),
                        spotColor = colors.primary.copy(alpha = 0.4f)
                    )
                    .background(
                        Brush.linearGradient(listOf(colors.primary, colors.tertiary)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appName.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onPrimary
                )
            }

            RexSpinner(size = 28.dp)

            Text(
                text = "Membuka $appName",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant
            )
        }
    }
}

/** A single lightweight rotating arc — cheaper than a Lottie/progress lib. */
@Composable
private fun RexSpinner(size: androidx.compose.ui.unit.Dp) {
    val colors = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "spinner")

    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing)
        ),
        label = "spinAngle"
    )

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = angle }
            .background(
                Brush.sweepGradient(
                    listOf(
                        Color.Transparent,
                        colors.primary.copy(alpha = 0.15f),
                        colors.primary,
                        colors.primary
                    )
                ),
                CircleShape
            )
    ) {
        Box(
            modifier = Modifier
                .padding((size.value * 0.16f).dp)
                .fillMaxSize()
                .background(colors.background, CircleShape)
        )
    }
}
