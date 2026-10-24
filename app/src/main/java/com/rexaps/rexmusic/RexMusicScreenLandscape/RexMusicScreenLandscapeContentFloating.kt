package com.rexaps.rexmusic

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun RexBadge(size: Dp, compact: Boolean, clock: MotionClock) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val tertiary = scheme.tertiary
    val ringBrush = remember(primary, tertiary) {
        Brush.sweepGradient(
            listOf(Color.Transparent, primary.copy(alpha = 0.9f), tertiary.copy(alpha = 0.9f), Color.Transparent)
        )
    }
    Box(
        modifier = Modifier
            .size(size)
            .drawBehind {
                val base = this.size.minDimension
                drawCircle(primary.copy(alpha = 0.08f + 0.08f * clock.pulse.value), radius = base * 0.85f)
                rotate(clock.slow.value * 360f * 3f) {
                    drawCircle(
                        brush = ringBrush,
                        radius = base / 2f + 4.dp.toPx(),
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                }
            }
            .clip(RoundedCornerShape(if (compact) 12.dp else 14.dp))
            .background(Brush.linearGradient(listOf(primary, tertiary))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "R",
            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            color = scheme.onPrimary,
            fontWeight = FontWeight.Black
        )
    }
}

// ───────────────────────── Floating audio dock ─────────────────────────

@Composable
internal fun FloatingAudioDock(clock: MotionClock, miniPlayer: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = primary.copy(alpha = 0.20f),
                spotColor = primary.copy(alpha = 0.28f)
            )
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        scheme.surface.copy(alpha = 0.97f),
                        scheme.surfaceVariant.copy(alpha = 0.88f),
                        scheme.surface.copy(alpha = 0.97f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        primary.copy(alpha = 0.38f),
                        scheme.outlineVariant.copy(alpha = 0.25f),
                        scheme.tertiary.copy(alpha = 0.38f)
                    )
                ),
                shape
            )
            .drawWithContent {
                drawContent()
                // Highlight tipis yang menyapu tepi atas dock
                val w = this.size.width
                val cx = w * (0.1f + 0.8f * clock.pulse.value)
                val y = 0.75.dp.toPx()
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, primary.copy(alpha = 0.75f), Color.Transparent),
                        startX = cx - w * 0.2f,
                        endX = cx + w * 0.2f
                    ),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
    ) {
        miniPlayer()
    }
}
