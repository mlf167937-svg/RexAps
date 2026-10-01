package com.rexaps.rexmusic.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexmusic.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Subtitles

@Stable
class PlayerActions(
    val onClose: () -> Unit,
    val onLyrics: () -> Unit,
    val onMore: () -> Unit,
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrev: () -> Unit,
    val onOpenPrevious: () -> Unit,
    val onSeek: (Float) -> Unit,
    val onToggleLike: () -> Unit,
    val onRepeat: () -> Unit,
    val onAutoplay: () -> Unit,
    val onQueue: () -> Unit,
    val onDownload: () -> Unit,
    val onCancelDownload: () -> Unit,
    val onDelete: () -> Unit
)

// ───────────────────────── Full-screen player ─────────────────────────

@Composable
fun MusicPlayerCard(
    track: RexTrack,
    state: RexMusicUiState,
    positionState: State<Long>,
    isLiked: Boolean,
    downloaded: Boolean,
    downloadStatus: DownloadStatus?,
    accent: Color?,
    onAccentFound: (Color) -> Unit,
    actions: PlayerActions,
    modifier: Modifier = Modifier
) {
    val busy = state.phase.isPlayerBusy
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        BoxWithConstraints(
            modifier
                .fillMaxSize()
                .playerBackground(accent)
                .pointerInput(Unit) { detectTapGestures(onTap = {}) }
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            val coverSize = minOf(maxWidth - 48.dp, maxHeight * 0.42f).coerceAtLeast(160.dp)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                PlayerTopBar(state, track, actions.onClose, actions.onLyrics, actions.onMore)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    PlayerCover(
                        cover = track.cover, title = track.title, size = coverSize,
                        busy = busy, playing = state.isPlaying, accent = accent,
                        onAccentFound = onAccentFound,
                        onSwipeNext = actions.onNext, onSwipePrev = actions.onOpenPrevious
                    )
                }
                Spacer(Modifier.height(28.dp))
                Column(Modifier.padding(horizontal = 24.dp)) {
                    TrackInfo(track.title, track.artist, isLiked, actions.onToggleLike)
                    Spacer(Modifier.height(12.dp))
                    SeekSection(positionState, state.durationMs, state.bufferedPercent, busy, actions.onSeek)
                    Spacer(Modifier.height(8.dp))
                    PlayerControls(state, busy, actions)
                    Spacer(Modifier.height(16.dp))
                    BottomActions(state, downloaded, downloadStatus, actions)
                }
                PlayerError(state.error, actions.onPlayPause)
                NeighborSection(state, actions.onOpenPrevious, actions.onNext)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun statusLabel(state: RexMusicUiState): String = when (state.phase) {
    LoadPhase.Resolving -> "MENYIAPKAN"
    LoadPhase.Preparing ->
        if (state.bufferedPercent in 1..99) "MEMUAT ${state.bufferedPercent}%" else "MEMUAT"
    LoadPhase.Buffering -> "BUFFERING"
    else -> if (state.nowPlayingOffline) "DIPUTAR OFFLINE" else "SEDANG DIPUTAR"
}

@Composable
private fun PlayerTopBar(
    state: RexMusicUiState,
    track: RexTrack,
    onClose: () -> Unit,
    onLyrics: () -> Unit,
    onMore: () -> Unit
) {
    val busy = state.phase.isPlayerBusy

    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RexIconButton(
            Icons.Default.KeyboardArrowDown,
            "Tutup player",
            onClose,
            size = 48.dp,
            iconSize = 32.dp,
            tint = Color.White
        )

        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when {
                    busy -> CircularProgressIndicator(
                        strokeWidth = 1.5.dp,
                        color = Color.White,
                        modifier = Modifier.size(16.dp)
                    )

                    state.isPlaying -> EqualizerBars(
                        color = Color.White,
                        barWidth = 3.dp
                    )

                    else -> Unit
                }

                Text(
                    statusLabel(state),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 1.5.sp
                )
            }

            Text(
                track.album,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        RexIconButton(
            Icons.Default.Subtitles,
            "Tampilkan lirik",
            onLyrics,
            size = 48.dp,
            tint = Color.White
        )

        RexIconButton(
            Icons.Default.MoreVert,
            "Opsi lagu",
            onMore,
            size = 48.dp,
            tint = Color.White
        )
    }
}

// ───────────────────────── Background ─────────────────────────

@Composable
fun Modifier.playerBackground(accent: Color?): Modifier {
    val base = accent ?: MaterialTheme.colorScheme.primary
    val animated = animateColorAsState(base, tween(700), label = "playerAccent")
    return this.drawBehind {
        val c = animated.value
        drawRect(
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to lerp(c, Color.Black, 0.35f),
                    0.55f to lerp(RexPlayerBase, c, 0.28f),
                    1f to RexPlayerBase
                )
            )
        )
    }
}

// ───────────────────────── Equalizer ─────────────────────────

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

// ───────────────────────── Cover ─────────────────────────

@Composable
private fun PlayerCover(
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

// ───────────────────────── Title & like ─────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackInfo(title: String, artist: String, isLiked: Boolean, onToggleLike: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                title, style = MaterialTheme.typography.headlineSmall, color = Color.White,
                maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.basicMarquee()
            )
            Spacer(Modifier.height(2.dp))
            Text(
                artist, style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        LikeButton(isLiked, onToggleLike)
    }
}

@Composable
private fun LikeButton(isLiked: Boolean, onToggle: () -> Unit) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(isLiked) {
        if (isLiked) {
            scale.snapTo(0.7f)
            scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        }
    }
    RexIconButton(
        icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        description = if (isLiked) "Hapus dari favorit" else "Tambah ke favorit",
        onClick = onToggle,
        size = 48.dp, iconSize = 28.dp,
        tint = if (isLiked) MaterialTheme.colorScheme.tertiary else Color.White.copy(alpha = 0.85f),
        modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
    )
}

// ───────────────────────── Seek ─────────────────────────

@Composable
private fun SeekSection(
    positionState: State<Long>,
    durationMs: Long,
    bufferedPercent: Int,
    busy: Boolean,
    onSeek: (Float) -> Unit
) {
    val positionMs = positionState.value
    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
    val muted = Color.White.copy(alpha = 0.6f)
    Column {
        if (durationMs <= 0L && busy) {
            Box(Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = Color.White, trackColor = Color.White.copy(alpha = 0.2f)
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
private fun SeekBar(progress: Float, buffered: Float, onSeek: (Float) -> Unit) {
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
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)))
        Box(Modifier.fillMaxWidth(buffered.coerceIn(0f, 1f)).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
        Box(Modifier.fillMaxWidth(progress).height(4.dp).clip(CircleShape).background(Color.White))
        Box(
            Modifier
                .offset { IntOffset((progress * widthPx - thumb.toPx() / 2).roundToInt(), 0) }
                .size(thumb)
                .background(Color.White, CircleShape)
        )
    }
}

// ───────────────────────── Controls ─────────────────────────

@Composable
private fun PlayerControls(state: RexMusicUiState, busy: Boolean, actions: PlayerActions) {
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
            size = 52.dp, iconSize = 36.dp, tint = Color.White
        )
        PlayPauseButton(state.isPlaying, busy, actions.onPlayPause)
        RexIconButton(
            Icons.Default.SkipNext, "Lagu berikutnya", actions.onNext,
            size = 52.dp, iconSize = 36.dp, tint = Color.White
        )
        RepeatControl(state.repeatTotal, state.repeatLeft, actions.onRepeat)
    }
}

@Composable
private fun PlayPauseButton(isPlaying: Boolean, busy: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val scale = rememberPressScale(source, 0.92f)
    Box(
        Modifier
            .size(72.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(CircleShape)
            .background(Color.White)
            .clickable(
                interactionSource = source, indication = LocalIndication.current,
                role = Role.Button, onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = when {
                busy -> 0
                isPlaying -> 1
                else -> 2
            },
            label = "playPause"
        ) { s ->
            when (s) {
                0 -> CircularProgressIndicator(
                    color = RexPlayerBase, strokeWidth = 3.dp, modifier = Modifier.size(28.dp)
                )
                1 -> Icon(
                    Icons.Default.Pause, contentDescription = "Jeda",
                    tint = RexPlayerBase, modifier = Modifier.size(40.dp)
                )
                else -> Icon(
                    Icons.Default.PlayArrow, contentDescription = "Putar",
                    tint = RexPlayerBase, modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
private fun ToggleControl(
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
            tint = if (active) accent else Color.White.copy(alpha = 0.8f),
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

@Composable
private fun RepeatControl(total: Int, left: Int, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.tertiary
    val on = total > 0
    Box(
        Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (on) Icons.Default.RepeatOne else Icons.Default.Repeat,
            contentDescription = if (on) "Ulangi lagu, sisa $left dari $total kali" else "Atur ulangi lagu",
            tint = if (on) accent else Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(26.dp)
        )
        if (on) {
            Text(
                "$left", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = RexPlayerBase,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp)
                    .background(accent, CircleShape)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                    .size(4.dp).background(accent, CircleShape)
            )
        }
    }
}

@Composable
private fun BottomActions(
    state: RexMusicUiState,
    downloaded: Boolean,
    status: DownloadStatus?,
    actions: PlayerActions
) {
    val queueCount = state.userQueue.size
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            downloaded -> ActionPill(Icons.Default.DownloadDone, "Tersimpan", true, actions.onDelete)
            status != null -> ActionPill(
                Icons.Default.Close,
                if (status.stage == DownloadStage.Downloading && status.percent >= 0)
                    "Mengunduh ${status.percent}%" else "Menyiapkan...",
                true, actions.onCancelDownload
            )
            else -> ActionPill(Icons.Default.Download, "Unduh", false, actions.onDownload)
        }
        ActionPill(
            Icons.AutoMirrored.Filled.QueueMusic,
            if (queueCount > 0) "Antrian $queueCount" else "Antrian",
            queueCount > 0, actions.onQueue
        )
    }
}

@Composable
private fun ActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.tertiary
    val fg = if (active) accent else Color.White
    Row(
        Modifier.height(40.dp).clip(CircleShape)
            .background(if (active) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.12f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

// ───────────────────────── Error & neighbors ─────────────────────────

@Composable
private fun PlayerError(error: String?, onRetry: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AnimatedVisibility(visible = !error.isNullOrBlank()) {
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            shape = RoundedCornerShape(12.dp), color = scheme.errorContainer
        ) {
            Row(
                Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Error", tint = scheme.onErrorContainer)
                Spacer(Modifier.width(10.dp))
                Text(
                    error.orEmpty(), modifier = Modifier.weight(1f),
                    color = scheme.onErrorContainer, style = MaterialTheme.typography.bodySmall
                )
                TextButton(onClick = onRetry) { Text("Coba lagi") }
            }
        }
    }
}

@Composable
private fun NeighborSection(state: RexMusicUiState, onPrevious: () -> Unit, onNext: () -> Unit) {
    val previous = state.previous
    val upNext = state.upNext
    if (previous == null && upNext == null && !state.autoplay) return
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (previous != null) {
            NeighborRow("SEBELUMNYA", previous, Icons.Default.SkipPrevious, onPrevious)
        }
        if (upNext != null) {
            NeighborRow(
                if (state.userQueue.isNotEmpty()) "DI ANTRIAN" else "SELANJUTNYA",
                upNext, Icons.Default.SkipNext, onNext
            )
        } else if (state.autoplay) {
            AutoplayRow(state.favoriteArtists, state.popularCount, state.offline.enabled, onNext)
        }
    }
}

@Composable
private fun NeighborRow(
    label: String,
    track: RexTrack,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverThumb(track.cover, 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            Text(
                track.title, style = MaterialTheme.typography.titleSmall, color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist, style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(icon, contentDescription = label, tint = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun AutoplayRow(favorites: List<String>, popularCount: Int, offlineMode: Boolean, onClick: () -> Unit) {
    val subtitle = when {
        offlineMode -> "Dipilih dari lagu unduhanmu"
        favorites.isNotEmpty() -> "Dicampur dari ${favorites.joinToString(", ")} & lagu populer"
        popularCount > 0 -> "Dipilih acak dari lagu populer"
        else -> "Dipilih otomatis untukmu"
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(RexHeroBrush),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("AUTOPLAY PINTAR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            Text("Lagu berikutnya untukmu", style = MaterialTheme.typography.titleSmall, color = Color.White)
            Text(
                subtitle, style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f), maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.SkipNext, contentDescription = "Lagu berikutnya", tint = Color.White.copy(alpha = 0.8f))
    }
}

private fun fmt(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
