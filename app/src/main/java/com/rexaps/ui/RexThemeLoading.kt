// /data/data/com.termux/files/home/RexAps/app/src/main/java/com/rexaps/ui/RexThemeLoading.kt
package com.rexaps.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberInfiniteTransition
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Modern "applying theme" overlay: a soft blurred scrim, a rotating
 * gradient ring, and a row of small color dots representing the target
 * theme's palette — communicates *what* is loading, not just *that*
 * something is loading.
 */
@Composable
fun RexThemeLoadingOverlay(
    targetOption: RexThemeOption,
    label: String = "Menerapkan tema",
    minDurationMs: Long = 380L,
    onFinished: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val spec = RexPaletteSpec.of(targetOption)

    LaunchedEffect(targetOption) {
        delay(minDurationMs)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.38f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceVariant,
            shadowElevation = 28.dp,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 30.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                RexGradientRing(
                    size = 46.dp,
                    primary = Color(spec.primary),
                    tertiary = Color(spec.tertiary)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = targetOption.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface
                    )
                }

                RexPalettePreviewDots(spec = spec)
            }
        }
    }
}

/**
 * Full-screen loader for opening RexFox / RexPanel / RexChat.
 * Gradient ring + app initial + label — reads as a real "launching an
 * app" moment rather than a generic spinner.
 */
@Composable
fun RexAppLoader(
    appName: String,
    minDurationMs: Long = 480L,
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
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {

            Box(contentAlignment = Alignment.Center) {

                RexGradientRing(
                    size = 92.dp,
                    primary = colors.primary,
                    tertiary = colors.tertiary,
                    strokeFraction = 0.09f
                )

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(
                            elevation = 14.dp,
                            shape = CircleShape,
                            ambientColor = colors.primary.copy(alpha = 0.4f),
                            spotColor = colors.primary.copy(alpha = 0.4f)
                        )
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = appName.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.onPrimary
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Membuka",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onBackground
                )
            }
        }
    }
}

/** Rotating gradient ring — a single graphicsLayer rotation, cheap to run. */
@Composable
private fun RexGradientRing(
    size: Dp,
    primary: Color,
    tertiary: Color,
    strokeFraction: Float = 0.16f
) {
    val transition = rememberInfiniteTransition(label = "ring")

    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing)
        ),
        label = "ringAngle"
    )

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = angle }
            .background(
                Brush.sweepGradient(
                    listOf(
                        Color.Transparent,
                        primary.copy(alpha = 0.12f),
                        tertiary,
                        primary
                    )
                ),
                CircleShape
            )
    ) {
        Box(
            modifier = Modifier
                .padding(size * strokeFraction)
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

/** Small row of dots previewing the incoming theme's key colors. */
@Composable
private fun RexPalettePreviewDots(spec: RexPaletteSpec) {
    val dotColors = listOf(
        Color(spec.primary),
        Color(spec.tertiary),
        Color(spec.background),
        Color(spec.card)
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        dotColors.forEach { dot ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(dot)
            )
        }
    }
}
