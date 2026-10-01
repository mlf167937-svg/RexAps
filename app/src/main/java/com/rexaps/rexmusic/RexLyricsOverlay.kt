package com.rexaps.rexmusic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.detectTapGestures

private const val AUTO_SCROLL_IDLE_MS = 3000L

/**
 * Overlay lirik premium: background foto cover (blur + scrim), piringan hitam berputar,
 * lirik synced dengan fade edge, tap baris untuk seek (opsional lewat [onSeekTo]).
 */
@Composable
fun RexLyricsOverlay(
    track: RexTrack,
    lyrics: Lyrics,
    loading: Boolean,
    error: String?,
    accent: Color?,
    positionState: State<Long>,
    durationMs: Long,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    onSeekTo: ((Long) -> Unit)? = null
) {
    val base = accent ?: MaterialTheme.colorScheme.primary

    Box(
        modifier
            .fillMaxSize()
            .background(RexPlayerBase)
            .pointerInput(Unit) { }
    ) {
        LyricsBackdrop(track.cover, base)

        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            LyricsHeader(onClose)
            NowSpinning(track, isPlaying, base)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    loading -> LyricsLoading()
                    !error.isNullOrBlank() -> LyricsMessage(error)
                    lyrics.isEmpty -> LyricsMessage("Lirik tidak tersedia untuk lagu ini", showIcon = true)
                    lyrics.hasSynced -> SyncedLyrics(lyrics, positionState, onSeekTo)
                    else -> PlainLyrics(lyrics.plain)
                }
            }
            LyricsFooter(positionState, durationMs, base, onSeekTo)
        }
    }
}

// ───────────────────────── Background ─────────────────────────

@Composable
private fun LyricsBackdrop(cover: String, accent: Color) {
    Box(Modifier.fillMaxSize()) {
        if (cover.isNotBlank()) {
            CoverImage(
                url = cover,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = 1.5f; scaleY = 1.5f; alpha = 0.9f }
                    .blur(48.dp),
                shape = RoundedCornerShape(0.dp)
            )
        }
        // tint warna cover + scrim supaya teks tetap terbaca
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to accent.copy(alpha = 0.35f),
                    0.45f to Color.Black.copy(alpha = 0.55f),
                    1f to Color.Black.copy(alpha = 0.92f)
                )
            )
        )
    }
}

// ───────────────────────── Header ─────────────────────────

@Composable
private fun LyricsHeader(onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RexIconButton(
            Icons.Default.KeyboardArrowDown, "Tutup lirik", onClose,
            size = 48.dp, iconSize = 32.dp, tint = Color.White
        )
        Text(
            "LIRIK",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
            letterSpacing = 3.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.size(48.dp))
    }
}

// ───────────────────────── Vinyl ─────────────────────────

@Composable
private fun NowSpinning(track: RexTrack, isPlaying: Boolean, accent: Color) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VinylDisc(track.cover, isPlaying, accent, size = 112.dp)
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (track.album.isNotBlank() && track.album != "Single") {
                Text(
                    track.album,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun VinylDisc(cover: String, spinning: Boolean, accent: Color, size: Dp) {
    // sudut disimpan sebagai state, hanya dibaca di graphicsLayer -> tanpa recompose per frame
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(spinning) {
        if (!spinning) return@LaunchedEffect
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) {
                    val dt = (now - last) / 1_000_000_000f
                    angle = (angle + dt * 36f) % 360f // 10 detik per putaran
                }
                last = now
            }
        }
    }

    Box(
        Modifier
            .size(size)
            .shadow(20.dp, CircleShape, ambientColor = accent, spotColor = accent)
            .graphicsLayer { rotationZ = angle }
            .clip(CircleShape)
            .background(Color(0xFF0B0B0F))
            .drawBehind {
                val r = this.size.minDimension / 2f
                // alur piringan
                var rr = r * 0.97f
                while (rr > r * 0.5f) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = rr,
                        style = Stroke(width = 1f)
                    )
                    rr -= r * 0.045f
                }
                // kilau (sheen) diagonal
                drawCircle(
                    brush = Brush.linearGradient(
                        0f to Color.White.copy(alpha = 0.16f),
                        0.35f to Color.Transparent,
                        0.65f to Color.Transparent,
                        1f to Color.White.copy(alpha = 0.10f),
                        start = Offset(0f, 0f),
                        end = Offset(this.size.width, this.size.height)
                    ),
                    radius = r
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // label tengah = cover
        Box(
            Modifier
                .size(size * 0.42f)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.6f))
        ) {
            CoverImage(cover, Modifier.fillMaxSize(), CircleShape, iconSize = 20.dp)
        }
        // lubang tengah
        Box(
            Modifier
                .size(size * 0.07f)
                .clip(CircleShape)
                .background(RexPlayerBase)
        )
    }
}

// ───────────────────────── States ─────────────────────────

@Composable
private fun LyricsLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun LyricsMessage(message: String, showIcon: Boolean = false) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showIcon) {
            Icon(
                Icons.Default.Lyrics, contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
        }
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

// ───────────────────────── Lyrics ─────────────────────────

/** Fade halus di tepi atas & bawah daftar lirik. */
private fun Modifier.fadeEdges(height: Dp = 56.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val h = height.toPx()
        drawRect(
            Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black, endY = h),
            size = androidx.compose.ui.geometry.Size(size.width, h),
            blendMode = BlendMode.DstIn
        )
        drawRect(
            Brush.verticalGradient(
                0f to Color.Black, 1f to Color.Transparent,
                startY = size.height - h, endY = size.height
            ),
            topLeft = Offset(0f, size.height - h),
            size = androidx.compose.ui.geometry.Size(size.width, h),
            blendMode = BlendMode.DstIn
        )
    }

@Composable
private fun PlainLyrics(text: String) {
    val lines = remember(text) { text.split("\n").filter { it.isNotBlank() } }
    LazyColumn(
        Modifier.fillMaxSize().fadeEdges(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 32.dp)
    ) {
        itemsIndexed(lines) { _, line ->
            Text(
                line,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun SyncedLyrics(
    lyrics: Lyrics,
    positionState: State<Long>,
    onSeekTo: ((Long) -> Unit)?
) {
    val listState: LazyListState = rememberLazyListState()
    var userScrollUntil by remember { mutableLongStateOf(0L) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (scrolling) userScrollUntil = System.currentTimeMillis() + AUTO_SCROLL_IDLE_MS
        }
    }

    // hanya berubah saat baris aktif ganti -> tidak recompose tiap tick posisi
    val activeIndex by remember(lyrics) {
        derivedStateOf {
            val list = lyrics.synced
            val pos = positionState.value
            var lo = 0
            var hi = list.size - 1
            var result = -1
            while (lo <= hi) {
                val mid = (lo + hi) ushr 1
                if (list[mid].timeMs <= pos) { result = mid; lo = mid + 1 } else hi = mid - 1
            }
            result
        }
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex < 0) return@LaunchedEffect
        while (System.currentTimeMillis() < userScrollUntil) delay(300)
        val offset = -(listState.layoutInfo.viewportSize.height / 3)
        listState.animateScrollToItem(activeIndex, offset)
    }

    LazyColumn(
        Modifier.fillMaxSize().fadeEdges(72.dp),
        state = listState,
        contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 120.dp, bottom = 220.dp)
    ) {
        itemsIndexed(lyrics.synced, key = { i, _ -> i }) { idx, line ->
            val distance = if (activeIndex < 0) 3 else kotlin.math.abs(idx - activeIndex)
            val active = idx == activeIndex
            val alpha by animateFloatAsState(
                targetValue = when {
                    active -> 1f
                    distance == 1 -> 0.55f
                    distance == 2 -> 0.4f
                    else -> 0.28f
                },
                animationSpec = tween(280), label = "lyricAlpha"
            )
            val scale by animateFloatAsState(
                if (active) 1f else 0.88f, tween(280), label = "lyricScale"
            )
            Text(
                text = line.text.ifBlank { "♪" },
                style = TextStyle(
                    fontSize = 26.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = if (active) androidx.compose.ui.graphics.Shadow(
                        color = Color.White.copy(alpha = 0.45f),
                        blurRadius = 24f
                    ) else null
                ),
                color = Color.White.copy(alpha = alpha),
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (onSeekTo != null) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSeekTo(line.timeMs) } else Modifier
                    )
                    .padding(vertical = 10.dp)
            )
        }
    }
}

// ───────────────────────── Footer ─────────────────────────

@Composable
private fun LyricsFooter(
    positionState: State<Long>,
    durationMs: Long,
    accent: Color,
    onSeekTo: ((Long) -> Unit)?
) {
    val pos = positionState.value
    val progress = if (durationMs > 0) (pos.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
            .padding(horizontal = 28.dp)
            .padding(top = 8.dp, bottom = 16.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(24.dp)
                .pointerInput(durationMs, onSeekTo) {
                    if (onSeekTo == null || durationMs <= 0) return@pointerInput
                    detectTapGestures { o ->
                        onSeekTo((o.x / size.width * durationMs).toLong().coerceIn(0L, durationMs))
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            )
            Box(
                Modifier.fillMaxWidth(progress).height(4.dp).clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Color.White, accent.copy(alpha = 0.9f))))
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmtLyrics(pos), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            Text(
                if (durationMs > 0) fmtLyrics(durationMs) else "--:--",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

private fun fmtLyrics(ms: Long): String {
    val sec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(sec / 60, sec % 60)
}
