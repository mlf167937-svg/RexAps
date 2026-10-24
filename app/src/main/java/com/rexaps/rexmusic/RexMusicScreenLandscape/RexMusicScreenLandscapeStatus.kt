package com.rexaps.rexmusic

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
internal fun DockStatusSignal(clock: MotionClock) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val offset = index * 0.23f
            val height = 3.5.dp + (clock.pulse.value * (5 + index % 2 * 2) + sin(clock.slow.value * TwoPi + offset) * 2f + 2f).dp
            Box(
                Modifier
                    .padding(horizontal = 1.5.dp)
                    .width(2.5.dp)
                    .height(height.coerceIn(3.dp, 12.dp))
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (index == 1) scheme.tertiary.copy(alpha = 0.8f) else scheme.primary.copy(alpha = 0.65f))
            )
        }
    }
    Text(
        text = "AUDIO ENGINE",
        style = MaterialTheme.typography.labelSmall,
        color = scheme.onSurfaceVariant.copy(alpha = 0.58f),
        maxLines = 1,
        modifier = Modifier.padding(top = 5.dp)
    )
}

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
