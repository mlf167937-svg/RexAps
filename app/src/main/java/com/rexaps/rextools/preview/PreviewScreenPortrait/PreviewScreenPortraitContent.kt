package com.rexaps.rextools.preview

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.rexaps.rextools.downloader.MediaDownloader
import com.rexaps.rextools.ui.*
import kotlinx.coroutines.launch

@Composable
internal fun PreviewScreenPortraitContent(onBack: () -> Unit = {},
    vm: PreviewViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val toast = rememberRexToast()
    var saving by remember { mutableStateOf(false) }

    val goBack: () -> Unit = {
        when (state.mode) {
            PreviewMode.INPUT -> onBack()
            PreviewMode.GRID -> vm.backToInput()
            PreviewMode.SINGLE -> vm.backToGrid()
        }
    }
    BackHandler(enabled = state.mode != PreviewMode.INPUT) { goBack() }

    val title = when (state.mode) {
        PreviewMode.INPUT -> "Media Preview"
        PreviewMode.GRID -> "Hasil Ekstraksi"
        PreviewMode.SINGLE -> "Pratinjau"
    }
    val subtitle = when (state.mode) {
        PreviewMode.INPUT -> "Tempel link, lihat medianya"
        PreviewMode.GRID -> "${state.items.size} item ditemukan"
        PreviewMode.SINGLE -> "Media ${state.currentIndex + 1} dari ${state.items.size}"
    }

    RexScreen(title = title, subtitle = subtitle, onBack = goBack, scroll = false, padded = false, toast = toast) {
        AnimatedContent(
            targetState = state.mode,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                if (targetState == PreviewMode.SINGLE || initialState == PreviewMode.SINGLE) {
                    (fadeIn(tween(320, delayMillis = 60)) +
                        scaleIn(tween(420, easing = RexEase), initialScale = if (forward) 0.92f else 1.06f)) togetherWith
                        (fadeOut(tween(180)) + scaleOut(tween(280), targetScale = if (forward) 1.06f else 0.92f))
                } else {
                    (fadeIn(tween(320, delayMillis = 60)) +
                        slideInHorizontally(tween(420, easing = RexEase)) { if (forward) it / 5 else -it / 5 }) togetherWith
                        (fadeOut(tween(180)) +
                            slideOutHorizontally(tween(300)) { if (forward) -it / 6 else it / 6 })
                }
            },
            label = "preview-mode"
        ) { mode ->
            when (mode) {
                PreviewMode.INPUT -> InputView(
                    input = state.rawInput,
                    error = state.error,
                    onInput = vm::onInputChange,
                    onExtract = vm::extract,
                    onClear = vm::clear
                )

                PreviewMode.GRID -> GridView(items = state.items, onOpen = { vm.openSingle(it) })

                PreviewMode.SINGLE -> {
                    val item = state.currentItem
                    if (item != null) {
                        SingleView(
                            item = item,
                            index = state.currentIndex,
                            total = state.items.size,
                            saving = saving,
                            onPrev = vm::prev,
                            onNext = vm::next,
                            onCopied = { toast.show("Link disalin") },
                            onOpenFail = { toast.show("Tidak bisa membuka link", true) },
                            onDownload = { url, type ->
                                if (!saving) {
                                    saving = true
                                    scope.launch {
                                        val res = if (type == MediaType.VIDEO) MediaDownloader.downloadVideo(ctx, url)
                                        else MediaDownloader.downloadImage(ctx, url)
                                        res.onSuccess { toast.show("Tersimpan ke perangkat") }
                                            .onFailure { toast.show("Gagal menyimpan: ${it.message}", true) }
                                        saving = false
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
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
