package com.rexaps.ui.motion

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Reusable micro-interactions. Callers can use the same helpers for cards, dialogs and module panels. */
fun rexFadeScaleIn(duration: Int = RexMotion.normal): EnterTransition =
    fadeIn(tween(duration, easing = FastOutSlowInEasing)) +
        scaleIn(initialScale = .965f, animationSpec = tween(duration, easing = FastOutSlowInEasing)) +
        slideInVertically(tween(duration, easing = FastOutSlowInEasing)) { it / 36 }

fun rexFadeScaleOut(duration: Int = RexMotion.fast): ExitTransition =
    fadeOut(tween(duration, easing = FastOutSlowInEasing)) +
        scaleOut(targetScale = .975f, animationSpec = tween(duration, easing = FastOutSlowInEasing)) +
        slideOutVertically(tween(duration, easing = FastOutSlowInEasing)) { it / 48 }

fun Modifier.rexPressScale(pressed: Boolean): Modifier = graphicsLayer {
    val target = if (pressed) .965f else 1f
    scaleX = target
    scaleY = target
}
