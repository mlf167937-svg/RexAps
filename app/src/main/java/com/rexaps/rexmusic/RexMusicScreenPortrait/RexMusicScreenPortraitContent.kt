package com.rexaps.rexmusic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
internal fun PortraitContentStage(
    selectedTab: Int,
    reduceMotion: Boolean,
    modifier: Modifier,
    content: @Composable (Modifier) -> Unit
) {
    val progress = remember { Animatable(1f) }
    var settledTab by remember { mutableStateOf(selectedTab) }
    var direction by remember { mutableStateOf(1f) }
    val shiftPx = with(LocalDensity.current) { 24.dp.toPx() }

    LaunchedEffect(selectedTab) {
        if (selectedTab != settledTab) {
            direction = if (selectedTab >= settledTab) 1f else -1f
            if (reduceMotion) {
                progress.snapTo(1f)
                settledTab = selectedTab
            } else {
                progress.snapTo(0f)
                settledTab = selectedTab
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 330, easing = PortraitEase)
                )
            }
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            val p = if (reduceMotion) 1f else progress.value
            alpha = p
            translationX = (1f - p) * shiftPx * direction
            val scale = 0.992f + p * 0.008f
            scaleX = scale
            scaleY = scale
        }
    ) {
        content(Modifier.fillMaxSize())
    }
}
