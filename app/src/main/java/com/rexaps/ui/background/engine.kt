package com.rexaps.ui.background

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.rexaps.ui.theme.RexAnimationLevel
import com.rexaps.ui.theme.RexDesignTokens
import kotlin.math.sin

/**
 * Premium theme-aware ambient renderer. All effects are drawn on one Canvas, so
 * animation does not trigger layout passes or allocate bitmap blur layers.
 */
@Composable
fun RexAmbientBackground(
    theme: RexDesignTokens,
    modifier: Modifier = Modifier,
    level: RexAnimationLevel = RexAnimationLevel.BALANCED,
    intensity: Float = .72f
) {
    val motionEnabled = level != RexAnimationLevel.OFF && intensity > 0f
    val transition = rememberInfiniteTransition(label = "rex-ambient")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = if (motionEnabled) 1f else 0f,
        animationSpec = if (motionEnabled) infiniteRepeatable(
            tween(if (level == RexAnimationLevel.SAVER) 26000 else 16500, easing = LinearEasing),
            RepeatMode.Reverse
        ) else tween(0),
        label = "ambient-phase"
    )
    val sweep by transition.animateFloat(
        initialValue = if (motionEnabled) -0.25f else 0f, targetValue = if (motionEnabled) 1.25f else 0f,
        animationSpec = if (motionEnabled) infiniteRepeatable(
            tween(if (level == RexAnimationLevel.SAVER) 30000 else 19000, easing = LinearEasing),
            RepeatMode.Reverse
        ) else tween(0),
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
        drawRect(brush = Brush.verticalGradient(listOf(Color(theme.background), Color(theme.backgroundAlt))))
        if (!motionEnabled || intensity <= 0f) return@Canvas

        val strength = intensity.coerceIn(0f, 1f)
        val alpha = strength * if (level == RexAnimationLevel.SAVER) .14f else .24f
        val primary = Color(theme.primary).copy(alpha = alpha)
        val accent = Color(theme.accent).copy(alpha = alpha * .62f)
        val secondary = Color(theme.secondary).copy(alpha = alpha * .42f)

        // Slowly drifting aurora clouds and a soft moving color sweep.
        drawRect(Brush.radialGradient(
            listOf(primary, primary.copy(alpha = alpha * .22f), Color.Transparent),
            center = Offset(w * (.14f + .52f * phase), h * (.12f + .06f * phase)),
            radius = min * 1.10f
        ))
        drawRect(Brush.radialGradient(
            listOf(accent, accent.copy(alpha = alpha * .20f), Color.Transparent),
            center = Offset(w * (.88f - .42f * phase), h * (.82f - .10f * phase)),
            radius = min * .90f
        ))
        drawRect(Brush.radialGradient(
            listOf(secondary, Color.Transparent),
            center = Offset(w * (.75f - .2f * phase), h * (.44f + .12f * phase)),
            radius = min * .60f
        ))
        drawRect(Brush.linearGradient(
            listOf(Color.Transparent, primary.copy(alpha = alpha * .25f), accent.copy(alpha = alpha * .18f), Color.Transparent),
            start = Offset(w * (sweep - .42f), 0f),
            end = Offset(w * (sweep + .42f), h)
        ))

        // Fine orbit rings provide depth; no heavy effects or offscreen compositing.
        val orbit = Offset(w * (.82f - .05f * phase), h * (.18f + .05f * phase))
        drawCircle(Color(theme.borderStrong).copy(alpha = alpha * .40f), min * .20f, orbit, style = Stroke(1.2f))
        drawCircle(Color(theme.primary).copy(alpha = alpha * .24f), min * .28f, orbit, style = Stroke(1f))
        drawCircle(Color(theme.secondary).copy(alpha = alpha * .18f), min * .35f, orbit, style = Stroke(1f))

        repeat(particleCount) { i ->
            val direction = if (i % 2 == 0) .12f else -.075f
            val x = ((i * 73f + sweep * w * direction) % w + w) % w
            val driftY = h * phase * if (i % 3 == 0) .035f else -.018f
            val y = ((i * 47f + driftY) % h + h) % h
            val radius = if (i % 7 == 0) 2.0f else 1.1f
            val particleColor = if (i % 3 == 0) accent else primary
            drawCircle(particleColor.copy(alpha = alpha * if (i % 5 == 0) .72f else .34f), radius, Offset(x, y))
        }

        // Category-specific motifs remain on this single Canvas to avoid extra layout/bitmap work.
        when (theme.category) {
            "Neon" -> {
                val step = (min / 12f).coerceAtLeast(30f)
                var x = 0f
                while (x < w) {
                    drawLine(Color(theme.accent).copy(alpha = alpha * .11f), Offset(x, 0f), Offset(x, h), 1f)
                    x += step
                }
                val scanY = h * ((sweep + 1f) / 2f).coerceIn(0f, 1f)
                drawLine(Color(theme.accent).copy(alpha = alpha * .22f), Offset(0f, scanY), Offset(w, scanY), 1.4f)
                drawLine(Color(theme.primary).copy(alpha = alpha * .09f), Offset(0f, scanY + 3f), Offset(w, scanY + 3f), 1f)
            }
            "Cosmic" -> {
                // Three softly breathing orbital ellipses and occasional brighter star points.
                drawCircle(Color(theme.accent).copy(alpha = alpha * .26f), min * .095f, Offset(w * (.18f + .05f * phase), h * (.32f - .03f * phase)), style = Stroke(1.1f))
                drawCircle(Color(theme.secondary).copy(alpha = alpha * .18f), min * .12f, Offset(w * (.18f + .05f * phase), h * (.32f - .03f * phase)), style = Stroke(.8f))
                repeat(8) { i ->
                    val x = (i * 127f + sweep * 0.14f * w).rem(w).let { if (it < 0f) it + w else it }
                    val y = (i * 83f + phase * h * .06f).rem(h)
                    drawCircle(Color(theme.accent).copy(alpha = alpha * if (i % 3 == 0) .42f else .18f), if (i % 3 == 0) 1.8f else 1.0f, Offset(x, y))
                }
            }
            "Light" -> {
                // Pearl-like highlights drift slowly instead of using a sharp grid.
                repeat(5) { i ->
                    val x = w * ((i * .21f + phase * .08f) % 1f)
                    val y = h * (0.16f + i * .17f + .018f * sin((phase * 6.283f + i).toDouble()).toFloat())
                    drawCircle(Color.White.copy(alpha = alpha * .17f), min * (if (i % 2 == 0) .018f else .012f), Offset(x, y))
                }
            }
            "Glass" -> {
                // Specular streak moves slowly over the glass surface, with a faint reflected edge.
                drawLine(Color.White.copy(alpha = alpha * .16f), Offset(w * (sweep * .72f), 0f), Offset(w * (sweep * .72f) - h * .16f, h), min * .0025f)
                drawLine(Color(theme.accent).copy(alpha = alpha * .10f), Offset(w * (sweep * .72f) + 4f, 0f), Offset(w * (sweep * .72f) - h * .16f + 4f, h), 1f)
            }
            "Elemental" -> {
                // Layered sine ribbons give each elemental palette a fluid / energetic undercurrent.
                repeat(3) { band ->
                    val baseY = h * (.36f + band * .17f)
                    val amp = min * (.018f + band * .004f)
                    val step = (w / 36f).coerceAtLeast(8f)
                    var x0 = 0f
                    var y0 = baseY + sin((phase * 6.283f + band).toDouble()).toFloat() * amp
                    var x = step
                    while (x <= w + step) {
                        val y = baseY + sin((x / w * 6.283f * (1.0 + band * .35) + phase * 6.283f + band).toDouble()).toFloat() * amp
                        drawLine(Color(if (band == 1) theme.secondary else theme.accent).copy(alpha = alpha * (.12f - band * .02f)), Offset(x0, y0), Offset(x, y), 1.1f)
                        x0 = x; y0 = y; x += step
                    }
                }
            }
            else -> {
                // A quiet diagonal contour gives dark themes depth without competing with content.
                drawLine(Color(theme.borderStrong).copy(alpha = alpha * .10f), Offset(0f, h * .72f), Offset(w * .32f, h), 1f)
                drawLine(Color(theme.primary).copy(alpha = alpha * .08f), Offset(w * .68f, 0f), Offset(w, h * .20f), 1f)
            }
        }
    }
}
