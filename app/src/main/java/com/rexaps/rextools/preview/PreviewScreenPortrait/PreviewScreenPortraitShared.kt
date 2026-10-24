// PriviewScreen.kt
package com.rexaps.rextools.preview

import androidx.compose.animation.core.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rexaps.rextools.ui.*
import kotlinx.coroutines.launch

@Composable
internal fun GridTile(item: PreviewItem, number: Int, index: Int, onClick: () -> Unit) {
    val s = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val desc = when (item.type) {
        MediaType.IMAGE -> "Gambar $number"
        MediaType.VIDEO -> "Video $number"
        MediaType.UNKNOWN -> "Link $number"
    }

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .entrance(index.coerceAtMost(12), 45, 18.dp)
            .pressScale(interaction, 0.95f)
            .clip(RexMd)
            .background(s.surfaceContainerHigh)
            .border(hairline(), RexMd)
            .semantics { contentDescription = desc }
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
    ) {
        if (item.type == MediaType.UNKNOWN) {
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                RexIconTile(Icons.Default.Link, size = 48.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    item.fileName,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                    color = s.onSurfaceVariant
                )
            }
        } else {
            RexImage(item.url, null, Modifier.fillMaxSize(), ContentScale.Crop)

            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(64.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
            )

            if (item.type == MediaType.VIDEO) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            Text(
                item.fileName,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Row(
            Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val (ic, label) = when (item.type) {
                MediaType.IMAGE -> Icons.Default.Image to "IMG"
                MediaType.VIDEO -> Icons.Default.Videocam to "VID"
                MediaType.UNKNOWN -> Icons.Default.Link to "URL"
            }
            Icon(ic, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

/* ═════════════════════ SINGLE VIEWER ═════════════════════ */

/** Pinch untuk zoom, geser saat diperbesar, ketuk dua kali untuk zoom cepat / reset. */
@Composable
internal fun ZoomableImage(url: String, description: String) {
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val offX = remember { Animatable(0f) }
    val offY = remember { Animatable(0f) }

    RexImage(
        model = url,
        contentDescription = description,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val ns = (scale.value * zoom).coerceIn(1f, 5f)
                    val maxX = size.width * (ns - 1f) / 2f
                    val maxY = size.height * (ns - 1f) / 2f
                    scope.launch {
                        scale.snapTo(ns)
                        if (ns > 1f) {
                            offX.snapTo((offX.value + pan.x).coerceIn(-maxX, maxX))
                            offY.snapTo((offY.value + pan.y).coerceIn(-maxY, maxY))
                        } else {
                            offX.snapTo(0f)
                            offY.snapTo(0f)
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    scope.launch {
                        if (scale.value > 1f) {
                            launch { scale.animateTo(1f, spring(0.8f)) }
                            launch { offX.animateTo(0f, spring(0.8f)) }
                            launch { offY.animateTo(0f, spring(0.8f)) }
                        } else {
                            scale.animateTo(2.5f, spring(0.8f))
                        }
                    }
                })
            }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                translationX = offX.value
                translationY = offY.value
            },
        contentScale = ContentScale.Fit
    )
}

/* ═════════════════════ EXOPLAYER ═════════════════════ */
