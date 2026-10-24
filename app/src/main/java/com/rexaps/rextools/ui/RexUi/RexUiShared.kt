package com.rexaps.rextools.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Muncul berurutan: fade + naik + sedikit membesar. */
internal fun Modifier.__splitImpl_entrance(index: Int = 0, delayPer: Int = 70, offset: Dp = 22.dp): Modifier = composed {
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

/** Skala menyusut halus saat ditekan (pegas). */
internal fun Modifier.__splitImpl_pressScale(source: InteractionSource, pressedScale: Float = 0.96f): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val sc by animateFloatAsState(
        if (pressed) pressedScale else 1f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    graphicsLayer { scaleX = sc; scaleY = sc }
}
