package com.rexaps.ui.motion

import androidx.compose.animation.*
import androidx.compose.animation.core.tween

/** Category-aware transitions remain short and use compositor-friendly alpha/translation. */
fun rexModuleEnter(category: String): EnterTransition = when(category.lowercase()) {
    "glass" -> fadeIn(tween(350)) + scaleIn(.98f, animationSpec = tween(350))
    "dark" -> fadeIn(tween(280)) + slideInHorizontally(tween(280)) { it / 18 }
    "light" -> fadeIn(tween(280)) + slideInHorizontally(tween(280)) { it / 24 }
    "neon" -> fadeIn(tween(260)) + slideInHorizontally(tween(260)) { it / 12 }
    "cosmic" -> fadeIn(tween(380)) + scaleIn(.96f, animationSpec = tween(380))
    "elemental" -> fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 24 }
    else -> fadeIn(tween(250))
}
