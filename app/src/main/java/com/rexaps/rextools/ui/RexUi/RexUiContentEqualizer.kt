package com.rexaps.rextools.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun RexEqualizer(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val t = rememberInfiniteTransition(label = "eq")
    val bars = listOf(520, 700, 430, 610).mapIndexed { i, ms ->
        t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(ms, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b$i")
    }
    Row(modifier.height(18.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        bars.forEach { f ->
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .graphicsLayer { scaleY = f.value; transformOrigin = TransformOrigin(0.5f, 1f) }
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
