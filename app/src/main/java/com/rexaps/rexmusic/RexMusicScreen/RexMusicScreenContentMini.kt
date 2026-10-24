package com.rexaps.rexmusic

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
internal fun MiniProgress(
    positionState: State<Long>,
    durationMs: Long,
    isPlaying: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val rawProgress = if (durationMs > 0L) {
        (positionState.value.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val progress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(
            durationMillis = if (isPlaying) 260 else 150,
            easing = LinearEasing
        ),
        label = "miniPlayerProgress"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(scheme.onSurface.copy(alpha = 0.11f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(scheme.primary, scheme.tertiary)
                    )
                )
        )
    }
}
