package com.rexaps.rexmusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin

/**
 * Portrait-first shell. It deliberately owns only visual chrome and layout; playback state,
 * tab data, and actions remain in the existing screen/controller. Every tint comes from the
 * host RexAps MaterialTheme, allowing the user's existing app palette to remain authoritative.
 */
@Composable
internal fun RexMusicScreenPortrait(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    miniPlayer: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val reduceMotion = portraitReduceMotion()
    val clock = rememberPortraitMotionClock(reduceMotion)

    Box(
        Modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        PortraitAmbientBackdrop(clock, Modifier.fillMaxSize())

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = snackbarHost,
            bottomBar = {
                AnimatedVisibility(
                    visible = selectedTab != TAB_SYNC,
                    enter = fadeIn(tween(220)) +
                        slideInVertically(tween(360, easing = PortraitEase)) { it / 2 } +
                        scaleIn(
                            animationSpec = tween(360, easing = PortraitEase),
                            initialScale = 0.975f,
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        ),
                    exit = fadeOut(tween(140)) +
                        slideOutVertically(tween(210, easing = PortraitEase)) { it / 3 } +
                        scaleOut(
                            animationSpec = tween(200, easing = PortraitEase),
                            targetScale = 0.985f,
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        )
                ) {
                    PortraitBottomChrome(
                        selectedTab = selectedTab,
                        onTabSelected = onTabSelected,
                        miniPlayer = miniPlayer,
                        clock = clock
                    )
                }
            }
        ) { padding ->
            PortraitContentStage(
                selectedTab = selectedTab,
                reduceMotion = reduceMotion,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                content = content
            )
        }
    }
}
