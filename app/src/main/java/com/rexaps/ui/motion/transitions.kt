package com.rexaps.ui.motion

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween

/** Category-aware page transitions: short, composited and tuned to each theme family. */
fun rexModuleEnter(category: String): EnterTransition = when (category.lowercase()) {
    "glass" -> fadeIn(tween(320, easing = LinearOutSlowInEasing)) +
        scaleIn(initialScale = .94f, animationSpec = tween(420, easing = LinearOutSlowInEasing)) +
        slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it / 28 }
    "dark" -> fadeIn(tween(240, easing = FastOutSlowInEasing)) +
        slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 20 } +
        scaleIn(initialScale = .985f, animationSpec = tween(300, easing = FastOutSlowInEasing))
    "light" -> fadeIn(tween(260, easing = LinearOutSlowInEasing)) +
        slideInVertically(tween(330, easing = FastOutSlowInEasing)) { it / 30 }
    "neon" -> fadeIn(tween(180, easing = FastOutSlowInEasing)) +
        slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { it / 12 } +
        scaleIn(initialScale = .97f, animationSpec = tween(220, easing = FastOutSlowInEasing))
    "cosmic" -> fadeIn(tween(360, easing = LinearOutSlowInEasing)) +
        scaleIn(initialScale = .92f, animationSpec = tween(480, easing = LinearOutSlowInEasing)) +
        slideInVertically(tween(420, easing = FastOutSlowInEasing)) { it / 22 }
    "elemental" -> fadeIn(tween(270, easing = FastOutSlowInEasing)) +
        slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it / 18 } +
        scaleIn(initialScale = .975f, animationSpec = tween(340, easing = FastOutSlowInEasing))
    else -> fadeIn(tween(240, easing = FastOutSlowInEasing)) +
        scaleIn(initialScale = .98f, animationSpec = tween(240, easing = FastOutSlowInEasing))
}

fun rexModuleExit(category: String): ExitTransition = when (category.lowercase()) {
    "glass", "cosmic" -> fadeOut(tween(180)) + scaleOut(targetScale = .97f, animationSpec = tween(220))
    "neon" -> fadeOut(tween(120)) + slideOutHorizontally(tween(160)) { -it / 18 }
    "elemental" -> fadeOut(tween(170)) + slideOutVertically(tween(210)) { -it / 24 }
    "light" -> fadeOut(tween(180)) + slideOutVertically(tween(190)) { -it / 32 }
    else -> fadeOut(tween(160)) + slideOutHorizontally(tween(180)) { -it / 28 }
}
