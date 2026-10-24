package com.rexaps.rexmusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Landscape shell futuristik: floating navigation dock dengan indikator animasi,
 * ambient backdrop, content stage sinematik, dan floating audio dock.
 * Seluruh warna/typography berasal dari MaterialTheme; state & playback tetap milik parent.
 */
@Composable
internal fun RexMusicScreenLandscape(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    miniPlayer: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val reduceMotion = rememberReduceMotion()
    val clock = rememberMotionClock(reduceMotion)
    val railVisible = selectedTab != TAB_SYNC

    BoxWithConstraints(Modifier.fillMaxSize().background(scheme.background)) {
        val showNavigationLabels = maxWidth >= 900.dp
        val compactHeight = maxHeight < 420.dp
        val railWidth = if (showNavigationLabels) 104.dp else 80.dp

        AmbientBackdrop(clock, Modifier.fillMaxSize())

        Row(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = railVisible,
                enter = fadeIn(tween(240)) +
                    slideInHorizontally(tween(340, easing = EmphasizedDecelerate)) { -it / 2 } +
                    expandHorizontally(tween(340, easing = EmphasizedDecelerate), expandFrom = Alignment.Start),
                exit = fadeOut(tween(140)) +
                    slideOutHorizontally(tween(220, easing = EmphasizedAccelerate)) { -it / 3 } +
                    shrinkHorizontally(tween(240, easing = EmphasizedAccelerate), shrinkTowards = Alignment.Start)
            ) {
                NavigationDock(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    showLabels = showNavigationLabels,
                    compactHeight = compactHeight,
                    width = railWidth,
                    clock = clock
                )
            }

            Scaffold(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                containerColor = Color.Transparent,
                snackbarHost = snackbarHost,
                bottomBar = {
                    AnimatedVisibility(
                        visible = railVisible,
                        enter = fadeIn(tween(260)) +
                            slideInVertically(tween(360, easing = EmphasizedDecelerate)) { it / 2 } +
                            expandVertically(tween(320, easing = EmphasizedDecelerate), expandFrom = Alignment.Bottom) +
                            scaleIn(tween(320, easing = EmphasizedDecelerate), initialScale = 0.92f, transformOrigin = TransformOrigin(0.5f, 1f)),
                        exit = fadeOut(tween(150)) +
                            slideOutVertically(tween(220, easing = EmphasizedAccelerate)) { it / 2 } +
                            shrinkVertically(tween(240, easing = EmphasizedAccelerate), shrinkTowards = Alignment.Bottom) +
                            scaleOut(tween(220, easing = EmphasizedAccelerate), targetScale = 0.94f, transformOrigin = TransformOrigin(0.5f, 1f))
                    ) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier
                                    .widthIn(max = 1000.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                FloatingAudioDock(clock) { miniPlayer() }
                            }
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    ContentStage(
                        selectedTab = selectedTab,
                        reduceMotion = reduceMotion,
                        content = content
                    )
                }
            }
        }
    }
}

// ───────────────────────── Content stage ─────────────────────────
