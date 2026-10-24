package com.rexaps.rexmusic.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rexaps.rexmusic.*
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
internal fun PlayerDetails(
    track: RexTrack,
    state: RexMusicUiState,
    positionState: State<Long>,
    isLiked: Boolean,
    downloaded: Boolean,
    downloadStatus: DownloadStatus?,
    busy: Boolean,
    actions: PlayerActions,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        TrackInfo(
            title = track.title,
            artist = track.artist,
            isLiked = isLiked,
            onToggleLike = actions.onToggleLike,
            lyrics = state.lyrics,
            positionMs = positionState.value,
            onLyrics = actions.onLyrics
        )
        Spacer(Modifier.height(12.dp))
        SeekSection(positionState, state.durationMs, state.bufferedPercent, busy, actions.onSeek)
        Spacer(Modifier.height(8.dp))
        PlayerControls(state, busy, actions)
        Spacer(Modifier.height(16.dp))
        BottomActions(state, downloaded, downloadStatus, actions)
        PlayerError(state.error, actions.onPlayPause)
        NeighborSection(state, actions.onOpenPrevious, actions.onNext)
    }
}

//  Cover 

@Composable
internal fun PlayerCover(
    cover: String,
    title: String,
    size: Dp,
    busy: Boolean,
    playing: Boolean,
    accent: Color?,
    onAccentFound: (Color) -> Unit,
    onSwipeNext: () -> Unit,
    onSwipePrev: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val threshold = with(LocalDensity.current) { 72.dp.toPx() }
    val currentNext by rememberUpdatedState(onSwipeNext)
    val currentPrev by rememberUpdatedState(onSwipePrev)
    val shape = RoundedCornerShape(14.dp)
    val glow = accent ?: MaterialTheme.colorScheme.primary
    val scale = animateFloatAsState(
        targetValue = if (playing || busy) 1f else 0.9f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "coverScale"
    )
    Box(
        Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                translationX = offset.value * 0.5f
                rotationZ = offset.value / 90f
                alpha = 1f - (abs(offset.value) / 1200f).coerceIn(0f, 0.4f)
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val v = offset.value
                        scope.launch {
                            offset.animateTo(
                                0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
                            )
                        }
                        if (v < -threshold) currentNext() else if (v > threshold) currentPrev()
                    },
                    onDragCancel = { scope.launch { offset.animateTo(0f) } },
                    onHorizontalDrag = { change, dx ->
                        change.consume()
                        scope.launch { offset.snapTo(offset.value + dx) }
                    }
                )
            }
            .shadow(24.dp, shape, ambientColor = glow, spotColor = glow)
            .clip(shape)
    ) {
        CoverImage(
            cover, Modifier.fillMaxSize(), shape,
            onColor = onAccentFound, contentDescription = "Cover $title", iconSize = 72.dp
        )
        AnimatedVisibility(visible = busy, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
            }
        }
    }
}

//  Controls 

@Composable
internal fun PlayerControls(state: RexMusicUiState, busy: Boolean, actions: PlayerActions) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToggleControl(
            icon = Icons.Default.AutoAwesome,
            description = if (state.autoplay) "Autoplay pintar aktif" else "Autoplay pintar mati",
            active = state.autoplay, onClick = actions.onAutoplay
        )
        RexIconButton(
            Icons.Default.SkipPrevious, "Lagu sebelumnya", actions.onPrev,
            size = 52.dp, iconSize = 36.dp, tint = MaterialTheme.colorScheme.onBackground
        )
        PlayPauseButton(state.isPlaying, busy, actions.onPlayPause)
        RexIconButton(
            Icons.Default.SkipNext, "Lagu berikutnya", actions.onNext,
            size = 52.dp, iconSize = 36.dp, tint = MaterialTheme.colorScheme.onBackground
        )
        RepeatControl(state.repeatTotal, state.repeatLeft, actions.onRepeat)
    }
}
