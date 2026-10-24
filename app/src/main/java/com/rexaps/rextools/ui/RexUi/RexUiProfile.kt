// RexUi.kt
package com.rexaps.rextools.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Avatar dengan cincin gradien yang berputar pelan. */
@Composable
fun RexAvatar(
    model: Any?,
    contentDescription: String,
    size: Dp,
    modifier: Modifier = Modifier,
    ring: Dp = 3.dp
) {
    val s = MaterialTheme.colorScheme
    val angle = rememberInfiniteTransition(label = "ring")
        .animateFloat(0f, 360f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "angle")
    val brush = remember(s.primary, s.secondary, s.tertiary) {
        Brush.sweepGradient(listOf(s.primary, s.secondary, s.tertiary, s.primary))
    }
    Box(
        modifier
            .size(size)
            .background(s.surface, CircleShape)
            .drawBehind {
                val w = ring.toPx()
                rotate(angle.value) {
                    drawCircle(brush, radius = this.size.minDimension / 2f - w / 2f, style = Stroke(w))
                }
            }
    ) {
        RexImage(
            model, contentDescription,
            Modifier.fillMaxSize().padding(ring + 4.dp).clip(CircleShape).background(s.surfaceVariant),
            ContentScale.Crop
        )
    }
}
