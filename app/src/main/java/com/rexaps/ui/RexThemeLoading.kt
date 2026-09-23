// /data/data/com.termux/files/home/RexAps/app/src/main/java/com/rexaps/ui/RexThemeLoading.kt
package com.rexaps.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Determinate "applying theme" overlay. A real progress ring that fills
 * over the actual duration, then confirms with a checkmark before the
 * card dismisses — communicates *what* is loading and *that it finished*,
 * instead of a generic spinner that just disappears.
 */
@Composable
fun RexThemeLoadingOverlay(
    targetOption: RexThemeOption,
    label: String = "Menerapkan tema",
    minDurationMs: Long = 620L,
    onFinished: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val spec = RexPaletteSpec.of(targetOption)

    val checkHoldMs = 260L
    val fillDurationMs = (minDurationMs - checkHoldMs).coerceAtLeast(200L)

    var target by remember { mutableStateOf(0f) }
    var showCheck by remember { mutableStateOf(false) }
    var cardVisible by remember { mutableStateOf(false) }

    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(fillDurationMs.toInt(), easing = FastOutSlowInEasing),
        label = "themeProgress"
    )

    LaunchedEffect(targetOption) {
        cardVisible = true
        target = 1f
        delay(fillDurationMs)
        showCheck = true
        delay(checkHoldMs)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = cardVisible,
            enter = fadeIn(tween(220)) + scaleIn(
                initialScale = 0.88f,
                animationSpec = tween(260, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.92f)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = colors.surfaceVariant,
                shadowElevation = 28.dp,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {

                    Box(contentAlignment = Alignment.Center) {
                        RexProgressRing(
                            ringSize = 52.dp,
                            progress = progress,
                            primary = Color(spec.primary),
                            tertiary = Color(spec.tertiary),
                            track = colors.outlineVariant.copy(alpha = 0.4f)
                        )

                        AnimatedVisibility(
                            visible = showCheck,
                            enter = scaleIn(
                                initialScale = 0.4f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + fadeIn(tween(120))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(spec.primary), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(spec.onPrimary),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (showCheck) "Selesai" else label,
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
}

/** Determinate ring: track + gradient arc that fills with real progress. */
@Composable
private fun RexProgressRing(
    ringSize: Dp,
    progress: Float,
    primary: Color,
    tertiary: Color,
    track: Color,
    strokeFraction: Float = 0.14f
) {
    Canvas(modifier = Modifier.size(ringSize)) {
        val stroke = ringSize.toPx() * strokeFraction
        drawArc(
            color = track,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            brush = Brush.sweepGradient(listOf(primary, tertiary, primary)),
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

/**
 * Full-screen loader for opening RexFox / RexPanel / RexChat. Indeterminate
 * (open time isn't known ahead), but with an entrance pop and a subtle
 * breathing icon so it reads as "alive" rather than frozen.
 */
@Composable
fun RexAppLoader(
    appName: String,
    minDurationMs: Long = 480L,
    onFinished: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(appName) {
        visible = true
        delay(minDurationMs)
        onFinished()
    }

    val breathing = rememberInfiniteTransition(label = "breathe")
    val breatheScale by breathing.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(240)) + scaleIn(
                initialScale = 0.9f,
                animationSpec = tween(280, easing = FastOutSlowInEasing)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {

                Box(contentAlignment = Alignment.Center) {

                    RexGradientRing(
                        ringSize = 92.dp,
                        primary = colors.primary,
                        tertiary = colors.tertiary,
                        strokeFraction = 0.09f
                    )

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .graphicsLayer {
                                scaleX = breatheScale
                                scaleY = breatheScale
                            }
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
}

/** Rotating gradient ring — a single graphicsLayer rotation, cheap to run. */
@Composable
private fun RexGradientRing(
    ringSize: Dp,
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
            .size(ringSize)
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
                .padding(ringSize * strokeFraction)
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

    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
