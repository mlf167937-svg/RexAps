// PriviewScreen.kt
package com.rexaps.rextools.preview

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
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
import coil.compose.SubcomposeAsyncImage
import com.rexaps.rextools.downloader.MediaDownloader
import com.rexaps.rextools.ui.*
import kotlinx.coroutines.launch

@Composable
fun PreviewScreen(
    onBack: () -> Unit = {},
    vm: PreviewViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

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

    RexScreen(title = title, subtitle = subtitle, onBack = goBack, scroll = false, padded = false) {
        AnimatedContent(
            targetState = state.mode,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 24 }) togetherWith fadeOut(tween(160))
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
                            onPrev = vm::prev,
                            onNext = vm::next,
                            onDownload = { url, type ->
                                scope.launch {
                                    val res = if (type == MediaType.VIDEO) MediaDownloader.downloadVideo(ctx, url)
                                    else MediaDownloader.downloadImage(ctx, url)
                                    res.onSuccess {
                                        Toast.makeText(ctx, "Tersimpan ke perangkat", Toast.LENGTH_SHORT).show()
                                    }.onFailure {
                                        Toast.makeText(ctx, "Gagal menyimpan: ${it.message}", Toast.LENGTH_SHORT).show()
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

/* ───────────────────────── INPUT ───────────────────────── */

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
            description = "URL tunggal, JSON, atau teks apa pun yang berisi link."
        )

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RexLg)
                .background(s.surfaceContainerLow.copy(alpha = 0.85f))
                .border(hairline(), RexLg)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RexOverline("Input", Modifier.weight(1f))
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
                modifier = Modifier.fillMaxSize(),
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

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            RexOutlinedButton("Bersihkan", null, onClear, Modifier.weight(1f).height(52.dp), enabled = input.isNotBlank())
            RexPrimaryButton(
                text = "Extract & Preview",
                icon = Icons.Default.Search,
                onClick = onExtract,
                enabled = input.isNotBlank(),
                modifier = Modifier.weight(2f)
            )
        }
    }
}

/* ───────────────────────── GRID ───────────────────────── */

@Composable
private fun GridView(items: List<PreviewItem>, onOpen: (Int) -> Unit) {
    val images = remember(items) { items.count { it.type == MediaType.IMAGE } }
    val videos = remember(items) { items.count { it.type == MediaType.VIDEO } }
    val others = items.size - images - videos

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (images > 0) RexPill("$images foto", icon = Icons.Default.Image)
                if (videos > 0) RexPill("$videos video", icon = Icons.Default.Videocam)
                if (others > 0) RexPill("$others link", icon = Icons.Default.Link)
            }
        }
        itemsIndexed(items, key = { _, it -> it.id }) { idx, item ->
            GridTile(item, number = idx + 1, onClick = { onOpen(idx) })
        }
    }
}

@Composable
private fun GridTile(item: PreviewItem, number: Int, onClick: () -> Unit) {
    val s = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(120), label = "tile-press")
    val desc = when (item.type) {
        MediaType.IMAGE -> "Gambar $number"
        MediaType.VIDEO -> "Video $number"
        MediaType.UNKNOWN -> "Link $number"
    }

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
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
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = s.onSurfaceVariant
                )
            }
        } else {
            SubcomposeAsyncImage(
                model = item.url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = s.primary)
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.BrokenImage, contentDescription = "Gagal memuat", tint = s.onSurfaceVariant)
                    }
                }
            )

            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(64.dp)
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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

/* ───────────────────────── SINGLE VIEWER ───────────────────────── */

@Composable
private fun SingleView(
    item: PreviewItem,
    index: Int,
    total: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDownload: (String, MediaType) -> Unit
) {
    val ctx = LocalContext.current
    val s = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
                .clip(RexLg)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(targetState = item.id, label = "stage") { _ ->
                when (item.type) {
                    MediaType.IMAGE -> SubcomposeAsyncImage(
                        model = item.url,
                        contentDescription = "Gambar ${index + 1} dari $total",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        loading = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                            }
                        },
                        error = {
                            Column(
                                Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.BrokenImage, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Gambar tidak bisa dimuat",
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )

                    MediaType.VIDEO -> ExoVideoPlayer(item.url)

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
            color = s.surface,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalIconButton(onClick = onPrev) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Sebelumnya")
                    }
                    LinearProgressIndicator(
                        progress = { if (total > 0) (index + 1f) / total else 0f },
                        modifier = Modifier.weight(1f).height(5.dp).clip(CircleShape),
                        trackColor = s.surfaceContainerHighest
                    )
                    FilledTonalIconButton(onClick = onNext) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Berikutnya")
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RexSm)
                        .background(s.surfaceContainerHigh)
                        .padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.url,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = s.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(item.url))
                        Toast.makeText(ctx, "Link disalin", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Salin link", modifier = Modifier.size(20.dp))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    RexOutlinedButton(
                        "Buka", Icons.Default.OpenInNew,
                        { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url))) },
                        Modifier.weight(1f).height(52.dp)
                    )
                    RexPrimaryButton(
                        text = "Simpan",
                        icon = Icons.Default.Download,
                        onClick = { onDownload(item.url, item.type) },
                        modifier = Modifier.weight(1.4f)
                    )
                }
            }
        }
    }
}

/* ───────────────────────── EXOPLAYER ───────────────────────── */

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