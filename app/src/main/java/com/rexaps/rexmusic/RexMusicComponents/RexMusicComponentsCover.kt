package com.rexaps.rexmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rexaps.rexmusic.player.EqualizerBars

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
