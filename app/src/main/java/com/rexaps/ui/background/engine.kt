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

/** Lightweight theme-aware ambient renderer. Draw-only animation avoids layout passes. */
@Composable
fun RexAmbientBackground(
    theme: RexDesignTokens,
    modifier: Modifier = Modifier,
    level: RexAnimationLevel = RexAnimationLevel.BALANCED,
    intensity: Float = .72f
) {
    val motionEnabled = level != RexAnimationLevel.OFF
    val transition = rememberInfiniteTransition(label = "rex-ambient")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if(level == RexAnimationLevel.SAVER) 24000 else 16000, easing = LinearEasing)),
        label = "ambient-phase"
    )
    Canvas(modifier.fillMaxSize()) {
        drawRect(brush = Brush.verticalGradient(listOf(Color(theme.background), Color(theme.backgroundAlt))))
        if (!motionEnabled) return@Canvas
        val alpha = (intensity.coerceIn(0f,1f) * if(level == RexAnimationLevel.SAVER) .12f else .22f)
        val primary = Color(theme.primary).copy(alpha = alpha)
        val accent = Color(theme.accent).copy(alpha = alpha * .55f)
        val p = phase * size.width
        drawCircle(primary, radius = size.minDimension * .24f, center = Offset(size.width * .78f + sin(phase*6.28f)*size.width*.025f, size.height*.18f))
        drawCircle(accent, radius = size.minDimension * .16f, center = Offset(size.width*.18f + p*.04f, size.height*.74f))
        if (theme.category == "Cosmic" || theme.category == "Neon" || theme.category == "Elemental") {
            repeat(if(level == RexAnimationLevel.FULL) 28 else if(level == RexAnimationLevel.BALANCED) 12 else 4) { i ->
                val x = ((i * 73f + phase * size.width * (if(i%2==0) .12f else -.08f)) % size.width + size.width) % size.width
                val y = (i * 47f % size.height)
                drawCircle(Color(theme.accent).copy(alpha = alpha*.7f), radius = if(i%4==0) 2.2f else 1.1f, center = Offset(x,y))
            }
        }
        drawCircle(Color(theme.borderStrong).copy(alpha = alpha*.4f), radius = size.minDimension*.32f,
            center = Offset(size.width*.8f,size.height*.2f), style = Stroke(width = 1.2f))
    }
}
