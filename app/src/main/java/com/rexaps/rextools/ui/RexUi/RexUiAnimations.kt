// RexUi.kt
package com.rexaps.rextools.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Kilau bergerak untuk placeholder loading. */
fun Modifier.shimmer(): Modifier = composed {
    val s = MaterialTheme.colorScheme
    val x = rememberInfiniteTransition(label = "shimmer")
        .animateFloat(-0.5f, 1.5f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "x")
    drawWithContent {
        drawContent()
        val w = size.width
        drawRect(
            Brush.linearGradient(
                listOf(Color.Transparent, s.onSurface.copy(alpha = 0.09f), Color.Transparent),
                start = Offset(w * x.value - w * 0.35f, 0f),
                end = Offset(w * x.value, size.height * 0.6f)
            )
        )
    }
}

/* ═════════════════════ BACKGROUND & SCREEN ═════════════════════ */
