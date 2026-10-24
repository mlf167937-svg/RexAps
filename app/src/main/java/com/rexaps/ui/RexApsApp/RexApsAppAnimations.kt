// RexApsApp.kt
package com.rexaps.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Avatar berputar dengan ring warna tema. */
@Composable
internal fun SpinningAvatar(isActive: Boolean, size: Dp = 132.dp) {

    val colors = MaterialTheme.colorScheme
    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = if (isActive) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "angle"
    )

    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = 20.dp,
                shape = CircleShape,
                ambientColor = colors.primary.copy(alpha = 0.4f),
                spotColor = colors.primary.copy(alpha = 0.4f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = angle }
                .background(
                    Brush.sweepGradient(
                        listOf(colors.primary, colors.tertiary, colors.primary)
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize(0.9f)
                .background(colors.background, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.9f)
                    .background(accentBrush(), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "R",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimary
                )
            }
        }
    }
}

/* ═════════════════════ Tema ═════════════════════ */
