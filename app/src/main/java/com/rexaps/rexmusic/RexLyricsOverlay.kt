package com.rexaps.rexmusic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val AUTO_SCROLL_IDLE_MS = 3000L

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
    modifier: Modifier = Modifier
) {
    val base = accent ?: MaterialTheme.colorScheme.primary
    val bg = remember(base) {
        Brush.verticalGradient(
            listOf(
                Color.Black,
                lerpColor(base, Color.Black, 0.7f),
                Color.Black
            )
        )
    }

    Box(
        modifier
            .fillMaxSize()
            .background(bg)
            .pointerInput(Unit) { }
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            LyricsHeader(track, onClose)
            Box(Modifier.fillMaxSize().weight(1f)) {
                when {
                    loading -> LyricsLoading()
                    !error.isNullOrBlank() -> LyricsError(error)
                    lyrics.isEmpty -> LyricsEmpty()
                    lyrics.hasSynced -> SyncedLyrics(
                        lyrics = lyrics,
                        positionState = positionState,
                        durationMs = durationMs,
                        accent = base
                    )
                    else -> PlainLyrics(lyrics.plain)
                }
            }
            LyricsFooter(positionState, durationMs)
        }
    }
}

@Composable
private fun LyricsHeader(track: RexTrack, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RexIconButton(
            Icons.Default.KeyboardArrowDown, "Tutup lirik", onClose,
            size = 48.dp, iconSize = 32.dp, tint = Color.White
        )
        Column(
            Modifier.weight(1f).padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "LIRIK", style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f), letterSpacing = 2.sp
            )
            Text(
                "${track.artist} · ${track.title}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun LyricsLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
    }
}

@Composable
private fun LyricsError(message: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LyricsEmpty() {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            "Lirik tidak tersedia untuk lagu ini",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PlainLyrics(text: String) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp)
    ) {
        items(text.split("\n").filter { it.isNotBlank() }) { line ->
            Text(
                line,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun SyncedLyrics(
    lyrics: Lyrics,
    positionState: State<Long>,
    durationMs: Long,
    accent: Color
) {
    val listState = rememberLazyListState()
    var userScrollUntil by remember { mutableLongStateOf(0L) }

    // deteksi scroll manual
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (scrolling) userScrollUntil = System.currentTimeMillis() + AUTO_SCROLL_IDLE_MS
            }
    }

    val posMs = positionState.value
    val activeIndex = remember(posMs, lyrics.synced) {
        val list = lyrics.synced
        var lo = 0
        var hi = list.size - 1
        var result = -1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            if (list[mid].timeMs <= posMs) {
                result = mid
                lo = mid + 1
            } else hi = mid - 1
        }
        result
    }

    // auto-scroll hybrid: kalau user gak scroll selama 3 detik, auto lagi
    LaunchedEffect(activeIndex, userScrollUntil) {
        if (activeIndex < 0) return@LaunchedEffect
        while (true) {
            val now = System.currentTimeMillis()
            if (now >= userScrollUntil) {
                listState.animateScrollToItem(
                    index = activeIndex,
                    scrollOffset = -200
                )
                break
            }
            delay(500)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 180.dp)
    ) {
        itemsIndexed(lyrics.synced, key = { i, _ -> i }) { idx, line ->
            val active = idx == activeIndex
            val alpha by animateFloatAsState(
                if (active) 1f else 0.35f,
                tween(220),
                label = "lyricAlpha"
            )
            val scale by animateFloatAsState(
                if (active) 1.05f else 1f,
                tween(220),
                label = "lyricScale"
            )
            Text(
                text = line.text,
                style = if (active)
                    MaterialTheme.typography.titleLarge
                else
                    MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = alpha),
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                    }
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun LyricsFooter(positionState: State<Long>, durationMs: Long) {
    val progress = if (durationMs > 0) {
        (positionState.value.toFloat() / durationMs).coerceIn(0f, 1f)
    } else 0f
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Box(
            Modifier.fillMaxWidth().height(3.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            Box(
                Modifier.fillMaxWidth(progress).fillMaxHeight()
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmtLyrics(positionState.value), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            Text(
                if (durationMs > 0) fmtLyrics(durationMs) else "--:--",
                style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

private fun fmtLyrics(ms: Long): String {
    val sec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(sec / 60, sec % 60)
}

private fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t
)
