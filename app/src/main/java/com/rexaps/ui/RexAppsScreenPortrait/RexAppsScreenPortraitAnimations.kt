// RexAppsScreenPortrait.kt
package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Latar gradient radial yang bergerak pelan. */
@Composable
internal fun ProAmbientBackdrop(modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val inf = rememberInfiniteTransition(label = "rexPremiumAmbient")
    val drift by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18500, easing = LinearEasing), RepeatMode.Reverse),
        label = "ambientDrift"
    )
    val sweep by inf.animateFloat(
        initialValue = -0.25f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(14500, easing = LinearEasing), RepeatMode.Reverse),
        label = "ambientSweep"
    )
    Box(modifier.drawBehind {
        val w = size.width
        val h = size.height
        val min = size.minDimension
        drawRect(c.background)

        // Three large, low-contrast color fields produce a living aurora without
        // bitmap blur, off-screen layers, or per-frame layout work.
        drawRect(Brush.radialGradient(
            colors = listOf(c.primary.copy(alpha = 0.23f), c.primary.copy(alpha = 0.045f), Color.Transparent),
            center = Offset(w * (0.12f + 0.58f * drift), h * (0.10f + 0.08f * drift)),
            radius = min * 1.12f
        ))
        drawRect(Brush.radialGradient(
            colors = listOf(c.tertiary.copy(alpha = 0.17f), c.tertiary.copy(alpha = 0.035f), Color.Transparent),
            center = Offset(w * (0.92f - 0.47f * drift), h * (0.84f - 0.12f * drift)),
            radius = min * 0.94f
        ))
        drawRect(Brush.radialGradient(
            colors = listOf(c.secondary.copy(alpha = 0.10f), Color.Transparent),
            center = Offset(w * (0.82f - 0.2f * drift), h * (0.40f + 0.1f * drift)),
            radius = min * 0.62f
        ))
        drawRect(Brush.linearGradient(
            colors = listOf(Color.Transparent, c.primary.copy(alpha = 0.055f), c.tertiary.copy(alpha = 0.035f), Color.Transparent),
            start = Offset(w * (sweep - 0.45f), 0f),
            end = Offset(w * (sweep + 0.45f), h)
        ))

        // Fine orbital rings add depth while staying subtle beneath content.
        val orbitCenter = Offset(w * (0.88f - 0.08f * drift), h * (0.17f + 0.06f * drift))
        drawCircle(c.primary.copy(alpha = 0.10f), radius = min * 0.19f, center = orbitCenter, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.1f))
        drawCircle(c.tertiary.copy(alpha = 0.075f), radius = min * 0.27f, center = orbitCenter, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
        drawCircle(c.outlineVariant.copy(alpha = 0.045f), radius = min * 0.34f, center = orbitCenter, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))

        // Deterministic floating dust: cheap Canvas circles, no allocations outside draw.
        repeat(30) { i ->
            val direction = if (i % 2 == 0) 0.12f else -0.075f
            val x = ((i * 71f + sweep * w * direction) % w + w) % w
            val y = ((i * 67f + drift * h * (if (i % 3 == 0) 0.035f else -0.018f)) % h + h) % h
            val radius = if (i % 7 == 0) 2.0f else 1.0f
            val alpha = if (i % 5 == 0) 0.16f else 0.075f
            drawCircle(c.primary.copy(alpha = alpha), radius = radius, center = Offset(x, y))
        }

        // Almost invisible grid anchors the futuristic look without competing with text.
        val step = 48.dp.toPx()
        var gx = 0f
        while (gx < w) {
            drawLine(c.onBackground.copy(alpha = 0.018f), Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
            gx += step
        }
        var gy = 0f
        while (gy < h) {
            drawLine(c.onBackground.copy(alpha = 0.018f), Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
            gy += step
        }
    })
}
