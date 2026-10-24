package com.rexaps.rextools.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/* ═════════════════════ MODIFIERS ═════════════════════ */

/** Skala menyusut halus saat ditekan (pegas). */
fun Modifier.pressScale(source: InteractionSource, pressedScale: Float = 0.96f): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val sc by animateFloatAsState(
        if (pressed) pressedScale else 1f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    graphicsLayer { scaleX = sc; scaleY = sc }
}

/** Muncul berurutan: fade + naik + sedikit membesar. */
fun Modifier.entrance(index: Int = 0, delayPer: Int = 70, offset: Dp = 22.dp): Modifier = composed {
    val progress = remember { Animatable(0f) }
    val px = with(LocalDensity.current) { offset.toPx() }
    LaunchedEffect(Unit) {
        delay((index * delayPer).toLong())
        progress.animateTo(1f, tween(560, easing = RexEase))
    }
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * px
        val sc = 0.97f + 0.03f * p
        scaleX = sc
        scaleY = sc
    }
}

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
