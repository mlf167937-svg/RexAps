package com.rexaps.rexmusic

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

@Composable
internal fun rememberMotionClock(reduceMotion: Boolean): MotionClock {
    return if (reduceMotion) {
        remember { MotionClock(mutableStateOf(0.18f), mutableStateOf(0.5f)) }
    } else {
        val transition = rememberInfiniteTransition(label = "rexLandscapeClock")
        val slow = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(28_000, easing = LinearEasing)),
            label = "rexClockSlow"
        )
        val pulse = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(7_000, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "rexClockPulse"
        )
        remember(slow, pulse) { MotionClock(slow, pulse) }
    }
}
