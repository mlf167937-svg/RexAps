package com.rexaps.ui.motion

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

fun rexFadeScaleIn(duration: Int = RexMotion.normal): EnterTransition =
    fadeIn(tween(duration, easing = FastOutSlowInEasing)) +
        scaleIn(initialScale = .98f, animationSpec = tween(duration, easing = FastOutSlowInEasing))
fun rexFadeScaleOut(duration: Int = RexMotion.fast): ExitTransition =
    fadeOut(tween(duration)) + scaleOut(targetScale = .98f, animationSpec = tween(duration))
fun Modifier.rexPressScale(pressed: Boolean): Modifier = graphicsLayer {
    val target = if (pressed) .97f else 1f
    scaleX = target; scaleY = target
}
