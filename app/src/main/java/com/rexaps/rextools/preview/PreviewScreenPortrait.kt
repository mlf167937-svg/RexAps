// PriviewScreen.kt
package com.rexaps.rextools.preview

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.rexaps.rextools.ui.*
import kotlinx.coroutines.launch

@Composable
fun PreviewScreenPortrait(
    onBack: () -> Unit = {},
    vm: PreviewViewModel = viewModel()
) {
        return __splitImpl_PreviewScreenPortrait(onBack, vm)
    }

/* ═════════════════════ INPUT ═════════════════════ */

@Composable
internal fun InputView(
    input: String,
    error: String?,
    onInput: (String) -> Unit,
    onExtract: () -> Unit,
    onClear: () -> Unit
) {
        return __splitImpl_InputView(input, error, onInput, onExtract, onClear)
    }

/* ═════════════════════ GRID ═════════════════════ */

@Composable
internal fun GridView(items: List<PreviewItem>, onOpen: (Int) -> Unit) {
    val images = remember(items) { items.count { it.type == MediaType.IMAGE } }
    val videos = remember(items) { items.count { it.type == MediaType.VIDEO } }
    val others = items.size - images - videos

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.padding(bottom = 4.dp).entrance(0), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (images > 0) RexPill("$images foto", icon = Icons.Default.Image)
                if (videos > 0) RexPill("$videos video", icon = Icons.Default.Videocam)
                if (others > 0) RexPill("$others link", icon = Icons.Default.Link)
            }
        }
        itemsIndexed(items, key = { _, it -> it.id }) { idx, item ->
            GridTile(item, number = idx + 1, index = idx, onClick = { onOpen(idx) })
        }
    }
}

@Composable
internal fun GridTile(item: PreviewItem, number: Int, index: Int, onClick: () -> Unit) {
        return __splitImpl_GridTile(item, number, index, onClick)
    }

/* ═════════════════════ SINGLE VIEWER ═════════════════════ */

@Composable
internal fun SingleView(
    item: PreviewItem,
    index: Int,
    total: Int,
    saving: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onCopied: () -> Unit,
    onOpenFail: () -> Unit,
    onDownload: (String, MediaType) -> Unit
) {
        return __splitImpl_SingleView(item, index, total, saving, onPrev, onNext, onCopied, onOpenFail, onDownload)
    }

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

@Composable
internal fun ExoVideoPlayer(url: String) {
    val ctx = LocalContext.current
    val exoPlayer = remember(url) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(url) { onDispose { exoPlayer.release() } }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { c ->
            PlayerView(c).apply {
                player = exoPlayer
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                useController = true
            }
        }
    )
}
