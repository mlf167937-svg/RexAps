package com.rexaps.ui.background

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.rexaps.ui.theme.RexAnimationLevel
import com.rexaps.ui.theme.RexDesignTokens

@Composable
fun RexAmbientBackground(
    theme: RexDesignTokens,
    modifier: Modifier = Modifier,
    level: RexAnimationLevel = RexAnimationLevel.BALANCED,
    intensity: Float = .72f
) {
    val motionEnabled =
        level != RexAnimationLevel.OFF && intensity > 0f

    if (!motionEnabled) {
        // Render background statis tanpa menjalankan animasi berulang.
        Canvas(modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(theme.background),
                        Color(theme.backgroundAlt)
                    )
                )
            )
        }
        return
    }

    RexAnimatedAmbientLayer(
        theme = theme,
        modifier = modifier,
        level = level,
        intensity = intensity
    )
}

@Composable
private fun RexAnimatedAmbientLayer(
    theme: RexDesignTokens,
    modifier: Modifier,
    level: RexAnimationLevel,
    intensity: Float
) {
    val transition =
        rememberInfiniteTransition(label = "rex-ambient")

    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween<Float>(
                durationMillis =
                    if (level == RexAnimationLevel.SAVER) {
                        26000
                    } else {
                        16500
                    },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient-phase"
    )

    val sweep by transition.animateFloat(
        initialValue = -0.25f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween<Float>(
                durationMillis =
                    if (level == RexAnimationLevel.SAVER) {
                        30000
                    } else {
                        19000
                    },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient-sweep"
    )

    val particleCount = when (level) {
        RexAnimationLevel.FULL -> 32
        RexAnimationLevel.BALANCED -> 20
        RexAnimationLevel.SAVER -> 7
        RexAnimationLevel.OFF -> 0
    }

    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val min = size.minDimension

        // Background utama mengikuti tema aktif.
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color(theme.background),
                    Color(theme.backgroundAlt)
                )
            )
        )

        val strength = intensity.coerceIn(0f, 1f)

        val alpha = strength *
            if (level == RexAnimationLevel.SAVER) {
                .14f
            } else {
                .24f
            }

        val primary =
            Color(theme.primary).copy(alpha = alpha)

        val accent =
            Color(theme.accent).copy(alpha = alpha * .62f)

        val secondary =
            Color(theme.secondary).copy(alpha = alpha * .42f)

        // Ambient glow primer.
        drawRect(
            Brush.radialGradient(
                listOf(
                    primary,
                    primary.copy(alpha = alpha * .22f),
                    Color.Transparent
                ),
                center = Offset(
                    w * (.14f + .52f * phase),
                    h * (.12f + .06f * phase)
                ),
                radius = min * 1.10f
            )
        )

        // Ambient glow sekunder.
        drawRect(
            Brush.radialGradient(
                listOf(
                    accent,
                    accent.copy(alpha = alpha * .20f),
                    Color.Transparent
                ),
                center = Offset(
                    w * (.88f - .42f * phase),
                    h * (.82f - .10f * phase)
                ),
                radius = min * .90f
            )
        )

        // Lapisan glow tambahan yang lebih halus.
        drawRect(
            Brush.radialGradient(
                listOf(
                    secondary,
                    Color.Transparent
                ),
                center = Offset(
                    w * (.75f - .2f * phase),
                    h * (.44f + .12f * phase)
                ),
                radius = min * .60f
            )
        )

        // Gradient sweep bergerak secara horizontal.
        drawRect(
            Brush.linearGradient(
                listOf(
                    Color.Transparent,
                    primary.copy(alpha = alpha * .25f),
                    accent.copy(alpha = alpha * .18f),
                    Color.Transparent
                ),
                start = Offset(
                    w * (sweep - .42f),
                    0f
                ),
                end = Offset(
                    w * (sweep + .42f),
                    h
                )
            )
        )

        // Orbit dekoratif.
        val orbit = Offset(
            w * (.82f - .05f * phase),
            h * (.18f + .05f * phase)
        )

        drawCircle(
            Color(theme.borderStrong).copy(
                alpha = alpha * .40f
            ),
            min * .20f,
            orbit,
            style = Stroke(1.2f)
        )

        drawCircle(
            Color(theme.primary).copy(
                alpha = alpha * .24f
            ),
            min * .28f,
            orbit,
            style = Stroke(1f)
        )

        drawCircle(
            Color(theme.secondary).copy(
                alpha = alpha * .18f
            ),
            min * .35f,
            orbit,
            style = Stroke(1f)
        )

        // Partikel ambient.
        repeat(particleCount) { i ->
            val direction =
                if (i % 2 == 0) .12f else -.075f

            val x =
                ((i * 73f + sweep * w * direction) % w + w) % w

            val driftY = h * phase *
                if (i % 3 == 0) {
                    .035f
                } else {
                    -.018f
                }

            val y =
                ((i * 47f + driftY) % h + h) % h

            val radius =
                if (i % 7 == 0) 2f else 1.1f

            val particleColor =
                if (i % 3 == 0) accent else primary

            drawCircle(
                particleColor.copy(
                    alpha = alpha *
                        if (i % 5 == 0) {
                            .72f
                        } else {
                            .34f
                        }
                ),
                radius,
                Offset(x, y)
            )
        }

        // Grid tipis hanya untuk tema Neon dan Cosmic.
        if (
            theme.category == "Neon" ||
            theme.category == "Cosmic"
        ) {
            val step =
                (min / 12f).coerceAtLeast(30f)

            var x = 0f

            while (x < w) {
                drawLine(
                    Color(theme.accent).copy(
                        alpha = alpha * .08f
                    ),
                    Offset(x, 0f),
                    Offset(x, h),
                    1f
                )

                x += step
            }
        }
    }
}