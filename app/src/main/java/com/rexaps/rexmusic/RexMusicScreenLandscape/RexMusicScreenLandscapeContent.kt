package com.rexaps.rexmusic

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
private fun rememberReduceMotion(): Boolean {
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
private fun rememberMotionClock(reduceMotion: Boolean): MotionClock {
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

// ───────────────────────── Root ─────────────────────────
/**
 * Landscape shell futuristik: floating navigation dock dengan indikator animasi,
 * ambient backdrop, content stage sinematik, dan floating audio dock.
 * Seluruh warna/typography berasal dari MaterialTheme; state & playback tetap milik parent.
 */

@Composable
internal fun RexMusicScreenLandscapeContent(selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    miniPlayer: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit) {
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

/**
 * Transisi tab tanpa menduplikasi / mereset content: `content` hanya dipanggil SEKALI,
 * efek sinematik (fade + slide + scale halus) dilakukan via graphicsLayer pada container.
 */
@Composable
private fun ContentStage(
    selectedTab: Int,
    reduceMotion: Boolean,
    content: @Composable (Modifier) -> Unit
) {
    val progress = remember { Animatable(1f) }
    var settledTab by remember { mutableStateOf(selectedTab) }
    var direction by remember { mutableStateOf(1f) }
    val shiftPx = with(LocalDensity.current) { 40.dp.toPx() }

    LaunchedEffect(selectedTab) {
        if (selectedTab != settledTab) {
            direction = if (selectedTab > settledTab) 1f else -1f
            if (reduceMotion) {
                settledTab = selectedTab
            } else {
                progress.snapTo(0f)
                settledTab = selectedTab
                progress.animateTo(1f, tween(420, easing = EmphasizedDecelerate))
            }
        }
    }

    Box(
        modifier = Modifier
            .widthIn(max = 1440.dp)
            .fillMaxSize()
            .graphicsLayer {
                // Frame pertama setelah tab berganti: tahan di alpha 0 agar tidak flicker.
                val switching = selectedTab != settledTab && !reduceMotion
                val p = if (switching) 0f else progress.value
                alpha = p
                translationX = (1f - p) * shiftPx * direction
                val s = 0.985f + 0.015f * p
                scaleX = s
                scaleY = s
            }
    ) {
        content(Modifier.fillMaxSize())
    }
}
