package com.rexaps.rexmusic.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rexaps.rexmusic.*

//  Full-screen player 

@Composable
internal fun MusicPlayerCardContent(track: RexTrack,
    state: RexMusicUiState,
    positionState: State<Long>,
    isLiked: Boolean,
    downloaded: Boolean,
    downloadStatus: DownloadStatus?,
    accent: Color?,
    onAccentFound: (Color) -> Unit,
    actions: PlayerActions,
    modifier: Modifier = Modifier) {
    val busy = state.phase.isPlayerBusy
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        BoxWithConstraints(
            modifier
                .fillMaxSize()
                .playerBackground(accent)
                .pointerInput(Unit) { detectTapGestures(onTap = {}) }
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            val landscape = maxWidth > maxHeight && maxWidth >= 560.dp
            val coverSize = if (landscape) {
                minOf(maxWidth * 0.36f, maxHeight - 100.dp).coerceAtLeast(128.dp)
            } else {
                minOf(maxWidth - 48.dp, maxHeight * 0.42f).coerceAtLeast(160.dp)
            }

            if (landscape) {
                Column(Modifier.fillMaxSize()) {
                    PlayerTopBar(state, track, actions.onClose, actions.onLyrics, actions.onMore)
                    Row(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.weight(0.92f).fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            PlayerCover(
                                cover = track.cover,
                                title = track.title,
                                size = coverSize,
                                busy = busy,
                                playing = state.isPlaying,
                                accent = accent,
                                onAccentFound = onAccentFound,
                                onSwipeNext = actions.onNext,
                                onSwipePrev = actions.onOpenPrevious
                            )
                        }
                        PlayerDetails(
                            track = track,
                            state = state,
                            positionState = positionState,
                            isLiked = isLiked,
                            downloaded = downloaded,
                            downloadStatus = downloadStatus,
                            busy = busy,
                            actions = actions,
                            modifier = Modifier
                                .weight(1.08f)
                                .fillMaxHeight()
                                .padding(horizontal = 20.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    PlayerTopBar(state, track, actions.onClose, actions.onLyrics, actions.onMore)
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        PlayerCover(
                            cover = track.cover,
                            title = track.title,
                            size = coverSize,
                            busy = busy,
                            playing = state.isPlaying,
                            accent = accent,
                            onAccentFound = onAccentFound,
                            onSwipeNext = actions.onNext,
                            onSwipePrev = actions.onOpenPrevious
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    PlayerDetails(
                        track = track,
                        state = state,
                        positionState = positionState,
                        isLiked = isLiked,
                        downloaded = downloaded,
                        downloadStatus = downloadStatus,
                        busy = busy,
                        actions = actions,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

//  Background 

@Composable
fun Modifier.playerBackground(accent: Color?): Modifier {
    val base = accent ?: MaterialTheme.colorScheme.primary
    val playerBase = RexPlayerBase
    val animated = animateColorAsState(base, tween(700), label = "playerAccent")
    return this.drawBehind {
        val c = animated.value
        drawRect(
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to lerp(playerBase, c, 0.14f),
                    0.55f to lerp(playerBase, c, 0.07f),
                    1f to playerBase
                )
            )
        )
    }
}

//  Equalizer 

@Composable
fun EqualizerBars(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    barWidth: Dp = 3.dp,
    height: Dp = 14.dp
) {
    val transition = rememberInfiniteTransition(label = "eq")
    val a by transition.animateFloat(
        0.3f, 1f, infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse), label = "a"
    )
    val b by transition.animateFloat(
        0.2f, 1f, infiniteRepeatable(tween(560, easing = LinearEasing), RepeatMode.Reverse), label = "b"
    )
    val c by transition.animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(340, easing = LinearEasing), RepeatMode.Reverse), label = "c"
    )
    Row(
        modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(a, b, c).forEach { fraction ->
            Box(
                Modifier.width(barWidth).fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(2.dp)).background(color)
            )
        }
    }
}

@Composable
internal fun ToggleControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.tertiary
    Box(
        Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, contentDescription = description,
            tint = if (active) accent else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.size(26.dp)
        )
        if (active) {
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                    .size(4.dp).background(accent, CircleShape)
            )
        }
    }
}
