package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal const val AUTO_SCROLL_IDLE_MS = 3000L

//  Lyrics 

/** Fade halus di tepi atas & bawah daftar lirik. */
internal fun Modifier.fadeEdges(height: Dp = 56.dp): Modifier = this
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

internal fun fmtLyrics(ms: Long): String {
    val sec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(sec / 60, sec % 60)
}
