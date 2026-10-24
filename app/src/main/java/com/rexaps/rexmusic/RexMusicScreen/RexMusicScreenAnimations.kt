package com.rexaps.rexmusic

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun MiniPlaybackPulse(color: Color) {
    // Infinite transitions exist only while the playing indicator is composed.
    val transition = rememberInfiniteTransition(label = "miniPlaybackPulse")
    val first by transition.animateFloat(
        initialValue = 4f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(430, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseBarOne"
    )
    val second by transition.animateFloat(
        initialValue = 10f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, delayMillis = 70, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseBarTwo"
    )
    val third by transition.animateFloat(
        initialValue = 5f,
        targetValue = 13f,
        animationSpec = infiniteRepeatable(
            animation = tween(390, delayMillis = 130, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseBarThree"
    )
    Row(
        modifier = Modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(first, second, third).forEach { height ->
            Box(
                Modifier
                    .width(2.5.dp)
                    .height(height.dp.coerceAtLeast(3.dp))
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}
