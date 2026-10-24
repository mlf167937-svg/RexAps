package com.rexaps.rexmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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
