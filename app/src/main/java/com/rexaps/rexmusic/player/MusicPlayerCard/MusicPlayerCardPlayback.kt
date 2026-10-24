package com.rexaps.rexmusic.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.rexaps.rexmusic.*
import kotlin.math.abs
import kotlin.math.roundToInt
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

//  Title & like 

@Composable
internal fun SeekSection(
    positionState: State<Long>,
    durationMs: Long,
    bufferedPercent: Int,
    busy: Boolean,
    onSeek: (Float) -> Unit
) {
    val positionMs = positionState.value
    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
    val muted = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    Column {
        if (durationMs <= 0L && busy) {
            Box(Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.onBackground, trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                )
            }
        } else {
            SeekBar(progress.coerceIn(0f, 1f), bufferedPercent / 100f, onSeek)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmt(positionMs), style = MaterialTheme.typography.labelSmall, color = muted)
            Text(
                if (durationMs > 0) fmt(durationMs) else "--:--",
                style = MaterialTheme.typography.labelSmall, color = muted
            )
        }
    }
}

@Composable
internal fun SeekBar(progress: Float, buffered: Float, onSeek: (Float) -> Unit) {
    val currentOnSeek by rememberUpdatedState(onSeek)
    var widthPx by remember { mutableFloatStateOf(1f) }
    val thumb = 14.dp
    Box(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(horizontal = thumb / 2)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { currentOnSeek((it.x / widthPx).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    currentOnSeek((change.position.x / widthPx).coerceIn(0f, 1f))
                }
            }
            .semantics {
                contentDescription = "Progres lagu"
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                setProgress { currentOnSeek(it.coerceIn(0f, 1f)); true }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f)))
        Box(Modifier.fillMaxWidth(buffered.coerceIn(0f, 1f)).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)))
        Box(Modifier.fillMaxWidth(progress).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onBackground))
        Box(
            Modifier
                .offset { IntOffset((progress * widthPx - thumb.toPx() / 2).roundToInt(), 0) }
                .size(thumb)
                .background(MaterialTheme.colorScheme.onBackground, CircleShape)
        )
    }
}

//  Controls 
