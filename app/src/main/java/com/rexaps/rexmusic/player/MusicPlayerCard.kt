package com.rexaps.rexmusic.player

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
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
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rexaps.rexmusic.LoadPhase
import com.rexaps.rexmusic.RexTrack
import com.rexaps.rexmusic.isPlayerBusy
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// ───────────────────────── Public entry ─────────────────────────

/**
 * Isi pemutar (tanpa background; background digambar halaman lewat [playerBackground]).
 * Posisi dibaca lewat State supaya hanya seek bar yang recompose tiap 500ms.
 */
@Composable
fun MusicPlayerCard(
    track: RexTrack,
    isPlaying: Boolean,
    positionState: State<Long>,
    durationMs: Long,
    isLiked: Boolean,
    accent: Color?,
    onAccentFound: (Color) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier,
    phase: LoadPhase = LoadPhase.Idle,
    bufferedPercent: Int = 0,
    repeatTotal: Int = 0,
    repeatLeft: Int = 0,
    autoplay: Boolean = true,
    queueCount: Int = 0,
    onRepeat: () -> Unit = {},
    onAutoplay: () -> Unit = {},
    onQueue: () -> Unit = {},
    onSwipePrev: () -> Unit = onPrev
) {
    PlayerCardContent(
        title = track.title, artist = track.artist, album = track.album, cover = track.cover,
        accent = accent, onAccentFound = onAccentFound,
        isPlaying = isPlaying, positionState = positionState, durationMs = durationMs,
        isLiked = isLiked, onPlayPause = onPlayPause, onNext = onNext, onPrev = onPrev,
        onSeek = onSeek, onToggleLike = onToggleLike,
        phase = phase, bufferedPercent = bufferedPercent,
        repeatTotal = repeatTotal, repeatLeft = repeatLeft, autoplay = autoplay,
        queueCount = queueCount, onRepeat = onRepeat, onAutoplay = onAutoplay, onQueue = onQueue,
        onSwipePrev = onSwipePrev, modifier = modifier
    )
}

/** Isi tanpa ketergantungan ke RexTrack (mudah di-preview). */
@Composable
private fun PlayerCardContent(
    title: String,
    artist: String,
    album: String,
    cover: String,
    accent: Color?,
    onAccentFound: (Color) -> Unit,
    isPlaying: Boolean,
    positionState: State<Long>,
    durationMs: Long,
    isLiked: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier,
    phase: LoadPhase = LoadPhase.Idle,
    bufferedPercent: Int = 0,
    repeatTotal: Int = 0,
    repeatLeft: Int = 0,
    autoplay: Boolean = true,
    queueCount: Int = 0,
    onRepeat: () -> Unit = {},
    onAutoplay: () -> Unit = {},
    onQueue: () -> Unit = {},
    onSwipePrev: () -> Unit = onPrev
) {
    val busy = phase.isPlayerBusy
    // Poster "bernapas": besar saat main, mengecil halus saat pause.
    val posterScale = animateFloatAsState(
        targetValue = if (isPlaying || busy) 1f else 0.9f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "posterScale"
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        PlayerHeader(album, statusLabel(phase, bufferedPercent), isPlaying, busy)
        AlbumPoster(cover, title, busy, accent, posterScale, onAccentFound, onNext, onSwipePrev)
        TrackInfo(title, artist, isLiked, onToggleLike)
        SeekSection(positionState, durationMs, bufferedPercent, busy, onSeek)
        PlayerControls(isPlaying, busy, onPlayPause, onNext, onPrev)
        SecondaryControls(repeatTotal, repeatLeft, autoplay, queueCount, onRepeat, onAutoplay, onQueue)
    }
}

/** Label status di header: berubah sesuai fase loading. */
private fun statusLabel(phase: LoadPhase, percent: Int): String = when (phase) {
    LoadPhase.Resolving -> "MENYIAPKAN"
    LoadPhase.Preparing -> if (percent in 1..99) "MEMUAT $percent%" else "MEMUAT"
    LoadPhase.Buffering -> "BUFFERING"
    else -> "NOW PLAYING"
}

// ───────────────────────── Background ─────────────────────────

/**
 * Background halaman: gradient dari warna cover ke background.
 * Warna dianimasikan dan dibaca di fase draw, jadi transisi warna tidak memicu recompose.
 */
@Composable
fun Modifier.playerBackground(accent: Color?): Modifier {
    val bg = MaterialTheme.colorScheme.background
    val base = accent ?: MaterialTheme.colorScheme.primary
    val animated = animateColorAsState(base, tween(700), label = "accent")
    return this.drawBehind {
        val c = animated.value
        drawRect(
            Brush.verticalGradient(
                listOf(
                    c.copy(alpha = 0.55f).compositeOver(bg),
                    c.copy(alpha = 0.20f).compositeOver(bg),
                    bg
                )
            )
        )
    }
}

/** Rata-rata warna dari bitmap 16x16 (cukup murah, tanpa library Palette). */
private fun Drawable.averageColor(): Color? = runCatching {
    val bmp = toBitmap(16, 16, Bitmap.Config.ARGB_8888)
    val px = IntArray(256)
    bmp.getPixels(px, 0, 16, 0, 0, 16, 16)
    var r = 0L; var g = 0L; var b = 0L
    px.forEach { r += (it shr 16 and 0xFF); g += (it shr 8 and 0xFF); b += (it and 0xFF) }
    Color((r / 256).toInt(), (g / 256).toInt(), (b / 256).toInt())
}.getOrNull()

// ───────────────────────── Equalizer (dipakai juga di layar utama) ─────────────────────────

/** 3 batang equalizer kecil. Hanya tampilkan saat lagu benar-benar main (animasi kontinu). */
@Composable
fun EqualizerBars(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    barWidth: Dp = 3.dp,
    height: Dp = 14.dp
) {
    val transition = rememberInfiniteTransition(label = "eq")
    val a by transition.animateFloat(
        0.3f, 1f,
        infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse), label = "a"
    )
    val b by transition.animateFloat(
        0.2f, 1f,
        infiniteRepeatable(tween(560, easing = LinearEasing), RepeatMode.Reverse), label = "b"
    )
    val c by transition.animateFloat(
        0.35f, 1f,
        infiniteRepeatable(tween(340, easing = LinearEasing), RepeatMode.Reverse), label = "c"
    )
    Row(
        modifier = modifier.height(height),
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

// ───────────────────────── Header & poster ─────────────────────────

/** Header: pill status (equalizer / spinner + label) dan nama album. */
@Composable
private fun PlayerHeader(album: String, status: String, playing: Boolean, busy: Boolean) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        StatusPill(status, playing, busy)
        Spacer(Modifier.height(6.dp))
        Text(album, style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold, maxLines = 1,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun StatusPill(label: String, playing: Boolean, busy: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.clip(CircleShape)
            .background(scheme.onSurface.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when {
            busy -> CircularProgressIndicator(
                strokeWidth = 1.5.dp, modifier = Modifier.size(10.dp), color = scheme.primary
            )
            playing -> EqualizerBars(color = scheme.primary, height = 10.dp, barWidth = 2.dp)
            else -> Box(
                Modifier.size(6.dp).clip(CircleShape)
                    .background(scheme.onSurfaceVariant.copy(alpha = 0.6f))
            )
        }
        Crossfade(targetState = label, label = "status") { text ->
            Text(text, style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant, letterSpacing = 1.5.sp)
        }
    }
}

/**
 * Poster 1:1: glow warna cover, scale saat pause, scrim + spinner saat loading.
 * Geser ke kiri = lagu berikutnya, geser ke kanan = lagu sebelumnya.
 */
@Composable
private fun AlbumPoster(
    cover: String,
    title: String,
    busy: Boolean,
    accent: Color?,
    scale: State<Float>,
    onAccentFound: (Color) -> Unit,
    onSwipeNext: () -> Unit,
    onSwipePrev: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val threshold = with(LocalDensity.current) { 72.dp.toPx() }
    val currentNext by rememberUpdatedState(onSwipeNext)
    val currentPrev by rememberUpdatedState(onSwipePrev)
    val shape = RoundedCornerShape(28.dp)
    val glow = accent ?: MaterialTheme.colorScheme.primary
    val request = remember(cover) {
        ImageRequest.Builder(context).data(cover).allowHardware(false).crossfade(true).build()
    }
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
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
            .shadow(28.dp, shape, ambientColor = glow, spotColor = glow)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (cover.isNotBlank()) {
            AsyncImage(
                model = request, contentDescription = "Cover $title",
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                onSuccess = { it.result.drawable.averageColor()?.let(onAccentFound) }
            )
        } else {
            Icon(Icons.Default.MusicNote, contentDescription = null,
                modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(visible = busy, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}

// ───────────────────────── Title, artist, like ─────────────────────────

/** Judul (marquee kalau kepanjangan), artist, dan tombol like. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackInfo(title: String, artist: String, isLiked: Boolean, onToggleLike: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.basicMarquee())
            Text(artist, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        LikeButton(isLiked, onToggleLike)
    }
}

/** Tombol like dengan animasi bounce saat berubah jadi liked. */
@Composable
private fun LikeButton(isLiked: Boolean, onToggle: () -> Unit) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(isLiked) {
        if (isLiked) {
            scale.snapTo(0.7f)
            scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        }
    }
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isLiked) "Hapus dari favorit" else "Tambah ke favorit",
            tint = if (isLiked) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        )
    }
}

// ───────────────────────── Seek bar ─────────────────────────

/** Seek bar + label waktu. Posisi dibaca di sini saja, jadi hanya bagian ini yang recompose. */
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
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column {
        if (durationMs <= 0L && busy) {
            IndeterminateSeekBar()
        } else {
            GradientSeekBar(progress.coerceIn(0f, 1f), bufferedPercent / 100f, onSeek)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmt(positionMs), style = MaterialTheme.typography.labelSmall, color = muted)
            Text(if (durationMs > 0) fmt(durationMs) else "--:--",
                style = MaterialTheme.typography.labelSmall, color = muted)
        }
    }
}

/** Ditampilkan saat lagu sedang dimuat dan durasi belum diketahui. */
@Composable
private fun IndeterminateSeekBar() {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = scheme.primary,
            trackColor = scheme.onSurface.copy(alpha = 0.2f)
        )
    }
}

/** Slider custom: track gradient, segmen buffer, thumb besar + shadow. Mendukung tap dan drag. */
@Composable
private fun GradientSeekBar(progress: Float, buffered: Float, onSeek: (Float) -> Unit) {
    val currentOnSeek by rememberUpdatedState(onSeek)
    var widthPx by remember { mutableFloatStateOf(1f) }
    val scheme = MaterialTheme.colorScheme
    val thumb = 18.dp
    Box(
        modifier = Modifier.fillMaxWidth().height(32.dp).padding(horizontal = thumb / 2)
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
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
            .background(scheme.onSurface.copy(alpha = 0.2f)))
        // segmen yang sudah ter-buffer
        Box(Modifier.fillMaxWidth(buffered.coerceIn(0f, 1f)).height(6.dp).clip(CircleShape)
            .background(scheme.onSurface.copy(alpha = 0.22f)))
        Box(Modifier.fillMaxWidth(progress).height(6.dp).clip(CircleShape)
            .background(Brush.horizontalGradient(listOf(scheme.primary, scheme.tertiary))))
        Box(
            Modifier
                .offset { IntOffset((progress * widthPx - thumb.toPx() / 2).roundToInt(), 0) }
                .size(thumb).shadow(4.dp, CircleShape).background(scheme.onSurface, CircleShape)
        )
    }
}

// ───────────────────────── Controls ─────────────────────────

/** Baris kontrol: prev, play/pause (besar, spinner saat loading), next. */
@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    loading: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SideControlButton(Icons.Default.SkipPrevious, "Lagu sebelumnya", onPrev)
        PlayPauseButton(isPlaying, loading, onPlayPause)
        SideControlButton(Icons.Default.SkipNext, "Lagu berikutnya", onNext)
    }
}

/** Tombol prev/next: lingkaran subtle dengan scale saat ditekan. */
@Composable
private fun SideControlButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val scale = rememberPressScale(source)
    val onSurface = MaterialTheme.colorScheme.onSurface
    IconButton(
        onClick = onClick,
        interactionSource = source,
        modifier = Modifier.size(56.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .background(onSurface.copy(alpha = 0.08f), CircleShape)
    ) {
        Icon(icon, contentDescription = description, tint = onSurface, modifier = Modifier.size(30.dp))
    }
}

/** Tombol play/pause 72dp: gradient, shadow berwarna, scale saat ditekan, spinner saat loading. */
@Composable
private fun PlayPauseButton(isPlaying: Boolean, loading: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val scale = rememberPressScale(source, pressed = 0.9f)
    val scheme = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        interactionSource = source,
        modifier = Modifier.size(72.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .shadow(14.dp, CircleShape, spotColor = scheme.primary)
            .background(Brush.linearGradient(listOf(scheme.primary, scheme.tertiary)), CircleShape)
    ) {
        Crossfade(
            targetState = when {
                loading -> 0
                isPlaying -> 1
                else -> 2
            },
            label = "playPauseIcon"
        ) { s ->
            when (s) {
                0 -> CircularProgressIndicator(
                    color = scheme.onPrimary, strokeWidth = 3.dp, modifier = Modifier.size(30.dp)
                )
                1 -> Icon(Icons.Default.Pause, contentDescription = "Jeda",
                    tint = scheme.onPrimary, modifier = Modifier.size(36.dp))
                else -> Icon(Icons.Default.PlayArrow, contentDescription = "Putar",
                    tint = scheme.onPrimary, modifier = Modifier.size(36.dp))
            }
        }
    }
}

/** Baris bawah: Ulangi (1-50x), Autoplay pintar, Antrian. */
@Composable
private fun SecondaryControls(
    repeatTotal: Int,
    repeatLeft: Int,
    autoplay: Boolean,
    queueCount: Int,
    onRepeat: () -> Unit,
    onAutoplay: () -> Unit,
    onQueue: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val repeatOn = repeatTotal > 0
        ActionPill(
            icon = if (repeatOn) Icons.Default.RepeatOne else Icons.Default.Repeat,
            label = if (repeatOn) "$repeatLeft/$repeatTotal×" else "Ulangi",
            description = if (repeatOn) "Ulangi lagu, sisa $repeatLeft dari $repeatTotal kali" else "Atur ulangi lagu",
            active = repeatOn, onClick = onRepeat, modifier = Modifier.weight(1f)
        )
        ActionPill(
            icon = Icons.Default.AutoAwesome,
            label = "Autoplay",
            description = if (autoplay) "Autoplay pintar aktif" else "Autoplay pintar mati",
            active = autoplay, onClick = onAutoplay, modifier = Modifier.weight(1f)
        )
        ActionPill(
            icon = Icons.AutoMirrored.Filled.QueueMusic,
            label = if (queueCount > 0) "Antrian $queueCount" else "Antrian",
            description = "Buka antrian",
            active = queueCount > 0, onClick = onQueue, modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionPill(
    icon: ImageVector,
    label: String,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        if (active) scheme.primary.copy(alpha = 0.18f) else scheme.onSurface.copy(alpha = 0.07f),
        label = "pillBg"
    )
    val fg by animateColorAsState(
        if (active) scheme.primary else scheme.onSurfaceVariant, label = "pillFg"
    )
    Row(
        modifier = modifier.clip(CircleShape).background(bg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Scale kecil saat ditekan; return State supaya dibaca di graphicsLayer. */
@Composable
private fun rememberPressScale(source: MutableInteractionSource, pressed: Float = 0.92f): State<Float> {
    val isPressed by source.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
}

private fun fmt(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

// ───────────────────────── Preview ─────────────────────────

@Preview(showBackground = true)
@Composable
private fun PlayerCardPreview() {
    MaterialTheme {
        val position = remember { mutableStateOf(72_000L) }
        Box(Modifier.playerBackground(Color(0xFF6650A4))) {
            PlayerCardContent(
                title = "Judul Lagu Yang Sangat Panjang Sekali Sampai Marquee",
                artist = "Nama Artis", album = "Nama Album", cover = "",
                accent = Color(0xFF6650A4), onAccentFound = {},
                isPlaying = true, positionState = position, durationMs = 215_000,
                isLiked = true, onPlayPause = {}, onNext = {}, onPrev = {}, onSeek = {},
                onToggleLike = {}, modifier = Modifier.padding(24.dp),
                phase = LoadPhase.Idle, bufferedPercent = 60,
                repeatTotal = 5, repeatLeft = 3, queueCount = 4
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlayerCardLoadingPreview() {
    MaterialTheme {
        val position = remember { mutableStateOf(0L) }
        PlayerCardContent(
            title = "Jatuh Suka", artist = "Tulus", album = "Manusia", cover = "",
            accent = Color(0xFF6650A4), onAccentFound = {},
            isPlaying = false, positionState = position, durationMs = 0,
            isLiked = false, onPlayPause = {}, onNext = {}, onPrev = {}, onSeek = {},
            onToggleLike = {}, modifier = Modifier.padding(24.dp),
            phase = LoadPhase.Preparing, bufferedPercent = 0
        )
    }
}
