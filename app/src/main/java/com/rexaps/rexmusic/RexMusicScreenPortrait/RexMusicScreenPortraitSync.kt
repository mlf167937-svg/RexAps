package com.rexaps.rexmusic

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
internal fun rememberPortraitMotionClock(reduceMotion: Boolean): PortraitMotionClock {
    if (reduceMotion) {
        return remember { PortraitMotionClock(mutableStateOf(0.18f), mutableStateOf(0.5f)) }
    }
    val transition = rememberInfiniteTransition(label = "rexPortraitAmbientClock")
    val drift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(31_000, easing = LinearEasing)),
        label = "rexPortraitAmbientDrift"
    )
    val pulse = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(5_800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "rexPortraitAmbientPulse"
    )
    return remember(drift, pulse) { PortraitMotionClock(drift, pulse) }
}
