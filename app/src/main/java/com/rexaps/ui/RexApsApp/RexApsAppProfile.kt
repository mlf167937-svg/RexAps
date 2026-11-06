package com.rexaps.ui

import com.rexaps.ui.theme.RexMotionRuntime
import com.rexaps.ui.theme.RexAnimationLevel

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
internal fun __splitImpl_SpinningAvatar(isActive: Boolean, size: Dp = 132.dp) {

    val colors = MaterialTheme.colorScheme
    val animationsEnabled = RexMotionRuntime.settings.level != RexAnimationLevel.OFF
    val angle = if (animationsEnabled) {
        val spin = rememberInfiniteTransition(label = "spin")
        val animatedAngle by spin.animateFloat(
            initialValue = 0f,
            targetValue = if (isActive) 360f else 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing)
            ),
            label = "angle"
        )
        animatedAngle
    } else 0f

    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = if (animationsEnabled) 20.dp else 0.dp,
                shape = CircleShape,
                ambientColor = colors.primary.copy(alpha = 0.4f),
                spotColor = colors.primary.copy(alpha = 0.4f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (animationsEnabled) Modifier.graphicsLayer { rotationZ = angle } else Modifier)
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
