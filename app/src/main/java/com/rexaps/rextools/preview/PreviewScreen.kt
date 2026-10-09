package com.rexaps.rextools.preview

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.item
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import com.rexaps.rextools.downloader.MediaDownloader
import kotlinx.coroutines.launch

/* ───────────────────────── DESIGN TOKENS ───────────────────────── */

private object Ui {
    // Spacing
    val s4 = 4.dp
    val s8 = 8.dp
    val s12 = 12.dp
    val s16 = 16.dp
    val s20 = 20.dp
    val s24 = 24.dp

    // Shape
    val rSm = RoundedCornerShape(12.dp)
    val rMd = RoundedCornerShape(16.dp)
    val rLg = RoundedCornerShape(20.dp)
    val rSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    // Touch target
    val button = 52.dp
}

@Composable
private fun accentBrush(): Brush {
    val s = MaterialTheme.colorScheme
    return Brush.linearGradient(listOf(s.primary, s.tertiary))
}

/* ───────────────────────── SCREEN ───────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                title = {
                    Column {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = goBack,
                        modifier = Modifier.padding(start = Ui.s8)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { pad ->
        AnimatedContent(
            targetState = state.mode,
            modifier = Modifier.fillMaxSize().padding(pad),
            transitionSpec = {
                (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 24 }) togetherWith
                    fadeOut(tween(160))
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

                PreviewMode.GRID -> GridView(
                    items = state.items,
                    onOpen = { vm.openSingle(it) }
                )

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
                                    val res = if (type == MediaType.VIDEO) {
                                        MediaDownloader.downloadVideo(ctx, url)
                                    } else {
                                        MediaDownloader.downloadImage(ctx, url)
                                    }
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

/* ───────────────────────── SHARED COMPONENTS ───────────────────────── */

/** Tombol utama dengan gradient aksen. */
@Composable
private fun GradientButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val brush = accentBrush()
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = Ui.rMd,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        modifier = modifier.height(Ui.button)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .then(if (enabled) Modifier.background(brush) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Ui.s8))
                Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Pill kecil untuk tipe media / statistik. */
@Composable
private fun InfoPill(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = content, fontWeight = FontWeight.Medium)
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
    val scheme = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = Ui.s20)
            .padding(bottom = Ui.s16)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(Ui.s16)
    ) {
        // Hero
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Ui.s16)
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .shadow(12.dp, Ui.rMd, ambientColor = scheme.primary, spotColor = scheme.primary)
                    .clip(Ui.rMd)
                    .background(accentBrush()),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = scheme.onPrimary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Ekstrak media dari mana saja",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "URL tunggal, JSON, atau teks apa pun yang berisi link.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        // Editor
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(Ui.rLg)
                .background(scheme.surfaceContainerLow)
                .border(BorderStroke(1.dp, scheme.outlineVariant), Ui.rLg)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = Ui.s16, end = Ui.s4, top = Ui.s4),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "INPUT",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.weight(1f)
                )
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

        // Error — ikon + teks, tidak bergantung warna saja
        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(Ui.rMd)
                    .background(scheme.errorContainer)
                    .padding(Ui.s12),
                horizontalArrangement = Arrangement.spacedBy(Ui.s12),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = scheme.onErrorContainer)
                Column {
                    Text(
                        "Tidak bisa mengekstrak",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onErrorContainer
                    )
                    Text(
                        error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onErrorContainer
                    )
                }
            }
        }

        // Aksi
        Row(horizontalArrangement = Arrangement.spacedBy(Ui.s12), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onClear,
                enabled = input.isNotBlank(),
                shape = Ui.rMd,
                modifier = Modifier.weight(1f).height(Ui.button)
            ) {
                Text("Bersihkan")
            }
            GradientButton(
                text = "Extract & Preview",
                icon = Icons.Default.AutoAwesome,
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
        contentPadding = PaddingValues(
            start = Ui.s16, end = Ui.s16, top = Ui.s4, bottom = Ui.s24
        ),
        horizontalArrangement = Arrangement.spacedBy(Ui.s12),
        verticalArrangement = Arrangement.spacedBy(Ui.s12)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier.padding(bottom = Ui.s4),
                horizontalArrangement = Arrangement.spacedBy(Ui.s8)
            ) {
                if (images > 0) InfoPill(Icons.Default.Image, "$images foto")
                if (videos > 0) InfoPill(Icons.Default.Videocam, "$videos video")
                if (others > 0) InfoPill(Icons.Default.Link, "$others link")
            }
        }
        itemsIndexed(items, key = { _, it -> it.id }) { idx, item ->
            GridTile(item, number = idx + 1, onClick = { onOpen(idx) })
        }
    }
}

@Composable
private fun GridTile(item: PreviewItem, number: Int, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
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
            .clip(Ui.rMd)
            .background(scheme.surfaceContainerHigh)
            .border(BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.6f)), Ui.rMd)
            .semantics { contentDescription = desc }
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
    ) {
        if (item.type == MediaType.UNKNOWN) {
            Column(
                Modifier.fillMaxSize().padding(Ui.s16),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(scheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = scheme.onPrimaryContainer)
                }
                Spacer(Modifier.height(Ui.s12))
                Text(
                    item.fileName,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = scheme.onSurfaceVariant
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
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = scheme.primary
                        )
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.BrokenImage,
                            contentDescription = "Gagal memuat",
                            tint = scheme.onSurfaceVariant
                        )
                    }
                }
            )

            // Scrim bawah agar teks selalu terbaca
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
                        .size(48.dp)
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
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = Ui.s12, vertical = Ui.s8)
            )
        }

        // Badge tipe (ikon + teks)
        Row(
            Modifier
                .align(Alignment.TopStart)
                .padding(Ui.s8)
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
    val scheme = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current

    Column(Modifier.fillMaxSize()) {

        // Stage
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = Ui.s12)
                .clip(Ui.rLg)
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
                                Spacer(Modifier.height(Ui.s8))
                                Text("Gambar tidak bisa dimuat", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    )

                    MediaType.VIDEO -> ExoVideoPlayer(item.url)

                    MediaType.UNKNOWN -> Column(
                        Modifier.padding(Ui.s24),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Ui.s12)
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

            // Penghitung
            Text(
                "${index + 1} / $total",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = Ui.s12)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }

        // Panel kontrol
        Surface(
            color = scheme.surface,
            shape = Ui.rSheet,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = Ui.s20, vertical = Ui.s16),
                verticalArrangement = Arrangement.spacedBy(Ui.s16)
            ) {
                // Navigasi
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Ui.s12)
                ) {
                    FilledTonalIconButton(onClick = onPrev) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Sebelumnya")
                    }
                    LinearProgressIndicator(
                        progress = { if (total > 0) (index + 1f) / total else 0f },
                        modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                        trackColor = scheme.surfaceContainerHighest
                    )
                    FilledTonalIconButton(onClick = onNext) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Berikutnya")
                    }
                }

                // URL
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(Ui.rSm)
                        .background(scheme.surfaceContainerHigh)
                        .padding(start = Ui.s12),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.url,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = scheme.onSurfaceVariant,
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

                // Aksi
                Row(horizontalArrangement = Arrangement.spacedBy(Ui.s12), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url))) },
                        shape = Ui.rMd,
                        modifier = Modifier.weight(1f).height(Ui.button)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Buka")
                    }
                    GradientButton(
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

    DisposableEffect(url) {
        onDispose {
            exoPlayer.release()
        }
    }

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
