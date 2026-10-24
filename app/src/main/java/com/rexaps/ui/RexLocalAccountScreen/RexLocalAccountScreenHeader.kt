package com.rexaps.ui

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun BannerView(acct: RexLocalAccount, rev: Int, modifier: Modifier, parallax: () -> Float) {
    val colors = MaterialTheme.colorScheme
    val bg = colors.background
    val inf = rememberInfiniteTransition(label = "banner")
    val kb by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "kb")
    val shift by inf.animateFloat(0f, 900f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "shift")
    var w by remember { mutableFloatStateOf(1f) }
    var h by remember { mutableFloatStateOf(1f) }
    val bmp by produceState<Bitmap?>(null, acct.bannerPath, rev) {
        value = if (acct.bannerPath.isNotBlank()) withContext(Dispatchers.IO) { decodeFileSampled(acct.bannerPath, 2048) } else null
    }
    val hasVideo = acct.bannerVideo.isNotBlank() && File(acct.bannerVideo).exists()

    Box(modifier.clipToBounds().onSizeChanged { w = it.width.toFloat(); h = it.height.toFloat() }) {
        Box(Modifier.fillMaxSize().graphicsLayer { translationY = parallax() }) {
            val b = bmp
            if (b != null) {
                Image(
                    b.asImageBitmap(), null,
                    Modifier.fillMaxSize().graphicsLayer {
                        if (!hasVideo) {
                            val z = 1.08f + 0.08f * kb
                            scaleX = z; scaleY = z
                            translationX = (kb - .5f) * 24f
                            translationY = (.5f - kb) * 12f
                        }
                    },
                    contentScale = ContentScale.Crop
                )
            } else if (!hasVideo) {
                Box(Modifier.fillMaxSize().drawBehind {
                    drawRect(
                        Brush.linearGradient(
                            listOf(colors.primary, colors.tertiary, colors.secondary),
                            start = Offset(shift, 0f),
                            end = Offset(shift + 900f, 900f),
                            tileMode = TileMode.Mirror
                        )
                    )
                })
            }
            if (hasVideo) {
                RexVideo(
                    acct.bannerVideo,
                    Modifier.fillMaxSize().graphicsLayer {
                        scaleX = acct.bannerScale; scaleY = acct.bannerScale
                        translationX = acct.bannerX * w
                        translationY = acct.bannerY * h
                    },
                    key = rev
                )
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            colorStops = arrayOf(0f to Color.Black.copy(.25f), .25f to Color.Transparent, .6f to Color.Transparent, 1f to bg)
        )))
    }
}
