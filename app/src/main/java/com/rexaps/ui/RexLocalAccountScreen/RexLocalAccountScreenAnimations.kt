// RexLocalAccountScreen.kt
package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player

@Composable
internal fun Reveal(visible: Boolean, delayMs: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible,
        enter = fadeIn(tween(550, delayMs)) + slideInVertically(tween(550, delayMs)) { it / 4 }
    ) { content() }
}

@Composable
internal fun GlowBadge(text: String) {
    Surface(shape = CircleShape, color = Color.White.copy(.16f)) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/* ═════════════════════ Video Player ═════════════════════ */
