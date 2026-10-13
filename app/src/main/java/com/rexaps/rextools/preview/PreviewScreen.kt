// PriviewScreen.kt
package com.rexaps.rextools.preview

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.rexaps.rextools.downloader.MediaDownloader
import com.rexaps.rextools.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PreviewScreen(
    onBack: () -> Unit = {},
    vm: PreviewViewModel = viewModel()
) {
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

/* ═════════════════════ INPUT ═════════════════════ */

@Composable
private fun InputView(
    input: String,
    error: String?,
    onInput: (String) -> Unit,
    onExtract: () -> Unit,
    onClear: () -> Unit
) {
    val s = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(if (focused) s.primary.copy(alpha = 0.8f) else s.outlineVariant, tween(300), label = "bd")
    val links = remember(input) { Regex("""https?://[^\s"'<>\\]+""").findAll(input).count() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 16.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RexHero(
            icon = Icons.Default.Link,
            title = "Ekstrak media dari mana saja",
            description = "URL tunggal, JSON, atau teks apa pun yang berisi link.",
            modifier = Modifier.entrance(0)
        )

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .entrance(1)
                .clip(RexLg)
                .background(s.surfaceContainerLow.copy(alpha = 0.85f))
                .border(1.dp, borderColor, RexLg)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RexOverline("Input")
                Spacer(Modifier.width(10.dp))
                AnimatedVisibility(
                    visible = links > 0,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) { RexPill("$links link terdeteksi", icon = Icons.Default.Link) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    clipboard.getText()?.text?.takeIf { it.isNotBlank() }?.let(onInput)
                }) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tempel")
                }
            }

            TextField(
                value = input,
                onValueChange = onInput,
                placeholder = {
                    Text(
                        "https://...  atau  {\"result\":[...]}",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxSize().onFocusChanged { focused = it.isFocused },
                minLines = 6,
                keyboardOptions = KeyboardOptions.Default,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        RexErrorSlot(error, "Tidak bisa mengekstrak")

        Row(Modifier.fillMaxWidth().entrance(2), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RexOutlinedButton("Bersihkan", null, onClear, Modifier.weight(1f), enabled = input.isNotBlank())
            RexPrimaryButton(
                text = "Extract & Preview",
                icon = Icons.Default.Bolt,
                onClick = onExtract,
                enabled = input.isNotBlank(),
                modifier = Modifier.weight(2f)
            )
        }
    }
}

/* ═════════════════════ GRID ═════════════════════ */

@Composable
private fun GridView(items: List<PreviewItem>, onOpen: (Int) -> Unit) {
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
private fun GridTile(item: PreviewItem, number: Int, index: Int, onClick: () -> Unit) {
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

@Composable
private fun SingleView(
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
    val ctx = LocalContext.current
    val s = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current
    var copied by remember(item.url) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }
    val progress by animateFloatAsState(
        if (total > 0) (index + 1f) / total else 0f,
        spring(stiffness = Spring.StiffnessMediumLow), label = "progress"
    )

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
                .clip(RexLg)
                .background(Color.Black)
                .border(hairline(0.5f), RexLg),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = index to item,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val fwd = targetState.first >= initialState.first
                    (slideInHorizontally(tween(400, easing = RexEase)) { if (fwd) it / 3 else -it / 3 } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(300)) { if (fwd) -it / 3 else it / 3 } + fadeOut(tween(200)))
                },
                label = "stage"
            ) { (i, current) ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (current.type) {
                        MediaType.IMAGE -> ZoomableImage(current.url, "Gambar ${i + 1} dari $total")
                        MediaType.VIDEO -> ExoVideoPlayer(current.url)
                        MediaType.UNKNOWN -> Column(
                            Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                            Text(
                                "Format tidak dikenali. Buka di browser untuk melihatnya.",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Text(
                "${index + 1} / $total",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }

        Surface(
            color = s.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            border = hairline(0.8f),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RexRoundButton(Icons.Default.ChevronLeft, "Sebelumnya", onPrev, size = 44.dp)
                    Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(s.surfaceContainerHighest)) {
                        Box(
                            Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(s.primary, s.tertiary)))
                        )
                    }
                    RexRoundButton(Icons.Default.ChevronRight, "Berikutnya", onNext, size = 44.dp)
                }

                Row(
                    Modifier.fillMaxWidth().clip(RexSm).background(s.surfaceContainerHigh).padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.url,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = s.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(item.url))
                        copied = true
                        onCopied()
                    }) {
                        AnimatedContent(
                            targetState = copied,
                            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.5f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.5f)) },
                            label = "copy"
                        ) { done ->
                            if (done) Icon(Icons.Default.Check, contentDescription = "Tersalin", tint = s.secondary, modifier = Modifier.size(20.dp))
                            else Icon(Icons.Default.ContentCopy, contentDescription = "Salin link", modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RexOutlinedButton(
                        "Buka", Icons.Default.OpenInNew,
                        {
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url))) }
                                .onFailure { onOpenFail() }
                        },
                        Modifier.weight(1f)
                    )
                    RexPrimaryButton(
                        text = "Simpan",
                        icon = Icons.Default.Download,
                        loading = saving,
                        loadingText = "Menyimpan…",
                        onClick = { onDownload(item.url, item.type) },
                        modifier = Modifier.weight(1.4f)
                    )
                }
            }
        }
    }
}

/** Pinch untuk zoom, geser saat diperbesar, ketuk dua kali untuk zoom cepat / reset. */
@Composable
private fun ZoomableImage(url: String, description: String) {
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
private fun ExoVideoPlayer(url: String) {
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