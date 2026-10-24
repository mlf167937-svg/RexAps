package com.rexaps.rexmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

//  Header 

@Composable
internal fun LyricsHeader(onClose: () -> Unit) {
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

//  Vinyl 

@Composable
internal fun NowSpinning(track: RexTrack, isPlaying: Boolean, accent: Color) {
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
internal fun VinylDisc(cover: String, spinning: Boolean, accent: Color, size: Dp) {
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

//  States 

@Composable
internal fun LyricsLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
    }
}

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
