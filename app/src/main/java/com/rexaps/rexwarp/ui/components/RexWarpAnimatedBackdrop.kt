package com.rexaps.rexwarp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme

/** Animated, theme-aware backdrop. It reads the active RexAps color scheme and never installs a theme. */
@Composable
fun RexWarpAnimatedBackdrop(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "rexwarp-backdrop")
    val drift by transition.animateFloat(
        initialValue = -0.12f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(9_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "backdrop-drift"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5_500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "backdrop-breathe"
    )

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(
                    colors.surface,
                    colors.surfaceContainerLow,
                    colors.surface
                )
            )
        )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.72f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.primary.copy(alpha = 0.14f * breathe), Color.Transparent),
                    center = Offset(size.width * (0.08f + drift), size.height * 0.13f),
                    radius = radius
                ),
                radius = radius,
                center = Offset(size.width * (0.08f + drift), size.height * 0.13f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.tertiary.copy(alpha = 0.10f * breathe), Color.Transparent),
                    center = Offset(size.width * (0.92f - drift), size.height * 0.84f),
                    radius = radius * 0.88f
                ),
                radius = radius * 0.88f,
                center = Offset(size.width * (0.92f - drift), size.height * 0.84f)
            )
        }
    }
}
