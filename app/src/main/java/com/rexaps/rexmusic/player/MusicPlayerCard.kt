package com.rexaps.rexmusic.player

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rexaps.rexmusic.RexTrack
import kotlin.math.roundToInt

// ───────────────────────── Public entry ─────────────────────────

/** Kartu player. Menyimpan warna aksen hasil ekstraksi cover, sisanya stateless. */
@Composable
fun MusicPlayerCard(
    track: RexTrack,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    isLiked: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    var accent by remember(track.cover) { mutableStateOf<Color?>(null) }
    PlayerCardContent(
        title = track.title, artist = track.artist, album = track.album, cover = track.cover,
        accent = accent, onAccentFound = { accent = it },
        isPlaying = isPlaying, positionMs = positionMs, durationMs = durationMs,
        isLiked = isLiked, onPlayPause = onPlayPause, onNext = onNext, onPrev = onPrev,
        onSeek = onSeek, onToggleLike = onToggleLike, modifier = modifier
    )
}

/** Isi kartu tanpa ketergantungan ke RexTrack (mudah di-preview). */
@Composable
private fun PlayerCardContent(
    title: String,
    artist: String,
    album: String,
    cover: String,
    accent: Color?,
    onAccentFound: (Color) -> Unit,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    isLiked: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brush = rememberPlayerBrush(accent)
    Column(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(brush)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        PlayerHeader(album)
        AlbumPoster(cover, title, onAccentFound)
        TrackInfo(title, artist, isLiked, onToggleLike)
        SeekSection(positionMs, durationMs, onSeek)
        PlayerControls(isPlaying, onPlayPause, onNext, onPrev)
    }
}

// ───────────────────────── Background ─────────────────────────

/** Gradient dari warna cover (atau primary) ke surface. Tanpa blur, ringan di HP low-end. */
@Composable
private fun rememberPlayerBrush(accent: Color?): Brush {
    val surface = MaterialTheme.colorScheme.surface
    val base = accent ?: MaterialTheme.colorScheme.primary
    val animated by animateColorAsState(base, tween(600), label = "accent")
    return remember(animated, surface) {
        Brush.verticalGradient(
            listOf(
                animated.copy(alpha = 0.45f).compositeOver(surface),
                animated.copy(alpha = 0.15f).compositeOver(surface),
                surface
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

// ───────────────────────── Header & poster ─────────────────────────

/** Header: label "NOW PLAYING" kecil, nama album di tengah, ikon more. */
@Composable
private fun PlayerHeader(album: String) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(24.dp))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("NOW PLAYING", style = MaterialTheme.typography.labelSmall,
                color = muted, letterSpacing = 1.5.sp)
            Text(album, style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
        Icon(Icons.Default.MoreVert, contentDescription = "Opsi lainnya", tint = muted)
    }
}

/** Poster 1:1 dengan shadow halus + border tipis; ekstrak warna saat gambar sukses dimuat. */
@Composable
private fun AlbumPoster(cover: String, title: String, onAccentFound: (Color) -> Unit) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(20.dp)
    val request = remember(cover) {
        ImageRequest.Builder(context).data(cover).allowHardware(false).crossfade(true).build()
    }
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            .shadow(16.dp, shape)
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
    }
}

// ───────────────────────── Title, artist, like ─────────────────────────

/** Judul (marquee kalau kepanjangan), artist, dan tombol like. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackInfo(title: String, artist: String, isLiked: Boolean, onToggleLike: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.basicMarquee())
            Text(artist, style = MaterialTheme.typography.bodyMedium,
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

/** Seek bar + label waktu. */
@Composable
private fun SeekSection(positionMs: Long, durationMs: Long, onSeek: (Float) -> Unit) {
    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column {
        GradientSeekBar(progress.coerceIn(0f, 1f), onSeek)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmt(positionMs), style = MaterialTheme.typography.labelSmall, color = muted)
            Text(fmt(durationMs), style = MaterialTheme.typography.labelSmall, color = muted)
        }
    }
}

/** Slider custom: track gradient, thumb besar + shadow. Mendukung tap dan drag. */
@Composable
private fun GradientSeekBar(progress: Float, onSeek: (Float) -> Unit) {
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

/** Baris kontrol: prev, play/pause (besar), next. */
@Composable
private fun PlayerControls(
    isPlaying: Boolean,
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
        PlayPauseButton(isPlaying, onPlayPause)
        SideControlButton(Icons.Default.SkipNext, "Lagu berikutnya", onNext)
    }
}

/** Tombol prev/next: lingkaran subtle dengan scale saat ditekan. */
@Composable
private fun SideControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val scale = rememberPressScale(source)
    val onSurface = MaterialTheme.colorScheme.onSurface
    IconButton(
        onClick = onClick,
        interactionSource = source,
        modifier = Modifier.size(52.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .background(onSurface.copy(alpha = 0.08f), CircleShape)
    ) {
        Icon(icon, contentDescription = description, tint = onSurface, modifier = Modifier.size(28.dp))
    }
}

/** Tombol play/pause 64dp: gradient, shadow berwarna, scale saat ditekan. */
@Composable
private fun PlayPauseButton(isPlaying: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val scale = rememberPressScale(source, pressed = 0.9f)
    val scheme = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        interactionSource = source,
        modifier = Modifier.size(64.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .shadow(12.dp, CircleShape, spotColor = scheme.primary)
            .background(Brush.linearGradient(listOf(scheme.primary, scheme.tertiary)), CircleShape)
    ) {
        Crossfade(targetState = isPlaying, label = "playPauseIcon") { playing ->
            Icon(
                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playing) "Jeda" else "Putar",
                tint = scheme.onPrimary, modifier = Modifier.size(32.dp)
            )
        }
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
        PlayerCardContent(
            title = "Judul Lagu Yang Sangat Panjang Sekali Sampai Marquee",
            artist = "Nama Artis", album = "Nama Album", cover = "",
            accent = Color(0xFF6650A4), onAccentFound = {},
            isPlaying = true, positionMs = 72_000, durationMs = 215_000,
            isLiked = true, onPlayPause = {}, onNext = {}, onPrev = {}, onSeek = {},
            onToggleLike = {}, modifier = Modifier.padding(16.dp)
        )
    }
}
