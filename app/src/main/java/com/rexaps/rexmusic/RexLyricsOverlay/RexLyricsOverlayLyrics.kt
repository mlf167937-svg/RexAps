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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
internal fun LyricsBody(
    lyrics: Lyrics,
    loading: Boolean,
    error: String?,
    positionState: State<Long>,
    onSeekTo: ((Long) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        when {
            loading -> LyricsLoading()
            !error.isNullOrBlank() -> LyricsMessage(error)
            lyrics.isEmpty -> LyricsMessage("Lirik tidak tersedia untuk lagu ini", showIcon = true)
            lyrics.hasSynced -> SyncedLyrics(lyrics, positionState, onSeekTo)
            else -> PlainLyrics(lyrics.plain)
        }
    }
}

//  Background 

@Composable
internal fun LyricsBackdrop(cover: String, accent: Color) {
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
                    0f to androidx.compose.ui.graphics.lerp(Color.Black, accent, 0.20f),
                    0.45f to Color.Black.copy(alpha = 0.82f),
                    1f to Color.Black.copy(alpha = 0.94f)
                )
            )
        )
    }
}

//  Header 

@Composable
internal fun LyricsMessage(message: String, showIcon: Boolean = false) {
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

//  Lyrics 

@Composable
internal fun PlainLyrics(text: String) {
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
internal fun SyncedLyrics(
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
                text = line.text.ifBlank { "" },
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

//  Footer 
