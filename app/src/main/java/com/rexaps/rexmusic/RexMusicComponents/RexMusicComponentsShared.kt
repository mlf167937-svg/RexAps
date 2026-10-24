package com.rexaps.rexmusic

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rexaps.rexmusic.player.EqualizerBars

class QuickTile(
    val id: String,
    val title: String,
    val cover: String,
    val special: Boolean = false,
    val onClick: () -> Unit
)

enum class CoverOverlay { None, Loading, Playing }

fun overlayFor(active: Boolean, playing: Boolean, loading: Boolean): CoverOverlay = when {
    active && loading -> CoverOverlay.Loading
    active && playing -> CoverOverlay.Playing
    else -> CoverOverlay.None
}

//  Helpers 

fun greetingForHour(hour: Int): String = when (hour) {
    in 4..10 -> "Selamat pagi"
    in 11..14 -> "Selamat siang"
    in 15..17 -> "Selamat sore"
    else -> "Selamat malam"
}

/** Gambar cover dengan placeholder ikon. Kalau [onColor] diisi, warna rata-rata dikirim balik. */
@Composable
fun CoverImage(
    url: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    onColor: ((Color) -> Unit)? = null,
    contentDescription: String? = null,
    iconSize: Dp = 24.dp
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val callback by rememberUpdatedState(onColor)
    Box(
        modifier.clip(shape).background(scheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.MusicNote, contentDescription = null,
            tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(iconSize)
        )
        if (url.isNotBlank()) {
            val needColor = onColor != null
            val request = remember(url, needColor) {
                ImageRequest.Builder(context).data(url)
                    .allowHardware(!needColor).crossfade(true).build()
            }
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { s -> callback?.let { cb -> s.result.drawable.averageColor()?.let(cb) } }
            )
        }
    }
}

/** Thumbnail kotak dengan overlay loading / equalizer. */
@Composable
fun CoverThumb(
    url: String,
    size: Dp,
    modifier: Modifier = Modifier,
    overlay: CoverOverlay = CoverOverlay.None,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Box(modifier.size(size)) {
        CoverImage(url, Modifier.fillMaxSize(), shape, iconSize = size / 2.5f)
        if (overlay != CoverOverlay.None) {
            Box(
                Modifier.fillMaxSize().clip(shape).background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                when (overlay) {
                    CoverOverlay.Loading -> CircularProgressIndicator(
                        color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(size / 2.4f)
                    )
                    CoverOverlay.Playing -> EqualizerBars(
                        color = Color.White, height = size / 3f, barWidth = 3.dp
                    )
                    CoverOverlay.None -> Unit
                }
            }
        }
    }
}

/** Saklar Online / Offline berbentuk pill. */
@Composable
fun ModeChip(offline: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val fg by animateColorAsState(
        if (offline) scheme.tertiary else scheme.onSurfaceVariant, label = "chipFg"
    )
    val bg by animateColorAsState(
        if (offline) scheme.tertiary.copy(alpha = 0.16f) else scheme.onSurface.copy(alpha = 0.08f),
        label = "chipBg"
    )
    Row(
        modifier.height(36.dp).clip(CircleShape).background(bg)
            .clickable(role = Role.Switch, onClick = onToggle)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            if (offline) Icons.Default.CloudOff else Icons.Default.Cloud,
            contentDescription = null, tint = fg, modifier = Modifier.size(16.dp)
        )
        Text(
            if (offline) "Offline" else "Online", color = fg,
            style = MaterialTheme.typography.labelLarge, maxLines = 1
        )
    }
}

@Composable
fun SkeletonList(modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val x by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f, targetValue = 900f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmerX"
    )
    val brush = Brush.linearGradient(
        colors = listOf(base.copy(alpha = 0.35f), base.copy(alpha = 0.9f), base.copy(alpha = 0.35f)),
        start = Offset(x - 300f, 0f), end = Offset(x, 0f)
    )
    Column(modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(7) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(brush))
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                    Box(Modifier.fillMaxWidth(0.4f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                }
            }
        }
    }
}

//  Track row (list) 

@Composable
fun QuickGrid(tiles: List<QuickTile>, nowId: String?, playing: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tiles.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { tile ->
                    QuickTileCard(tile, tile.id == nowId, playing, Modifier.weight(1f))
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
