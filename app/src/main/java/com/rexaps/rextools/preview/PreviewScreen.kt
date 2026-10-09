package com.rexaps.rextools.preview

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.rexaps.rextools.downloader.MediaDownloader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    onBack: () -> Unit = {},
    vm: PreviewViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (state.mode) {
                            PreviewMode.INPUT -> "Media Preview"
                            PreviewMode.GRID -> "Media Preview · ${state.items.size}"
                            PreviewMode.SINGLE -> "Media ${state.currentIndex + 1}/${state.items.size}"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (state.mode) {
                            PreviewMode.INPUT -> onBack()
                            PreviewMode.GRID -> vm.backToInput()
                            PreviewMode.SINGLE -> vm.backToGrid()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "back")
                    }
                }
            )
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (state.mode) {
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
                                        Toast.makeText(ctx, "tersimpan", Toast.LENGTH_SHORT).show()
                                    }.onFailure {
                                        Toast.makeText(ctx, "gagal: ${it.message}", Toast.LENGTH_SHORT).show()
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

// ───────────────────────── INPUT ─────────────────────────

@Composable
private fun InputView(
    input: String,
    error: String?,
    onInput: (String) -> Unit,
    onExtract: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Paste URL tunggal, JSON, atau teks apa aja yang berisi link.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = input,
            onValueChange = onInput,
            label = { Text("Input") },
            placeholder = { Text("https://...  atau  {\"result\":[...]}") },
            modifier = Modifier.fillMaxWidth().weight(1f),
            minLines = 6,
            maxLines = 20
        )

        error?.let {
            Text("Error: $it", color = MaterialTheme.colorScheme.error)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onClear,
                enabled = input.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Bersihkan")
            }
            Button(
                onClick = onExtract,
                enabled = input.isNotBlank(),
                modifier = Modifier.weight(2f)
            ) {
                Text("Extract & Preview")
            }
        }
    }
}

// ───────────────────────── GRID ─────────────────────────

@Composable
private fun GridView(items: List<PreviewItem>, onOpen: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(items, key = { _, it -> it.id }) { idx, item ->
            GridTile(item, onClick = { onOpen(idx) })
        }
    }
}

@Composable
private fun GridTile(item: PreviewItem, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        if (item.type == MediaType.UNKNOWN) {
            Column(
                Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Link, contentDescription = null, tint = scheme.primary)
                Spacer(Modifier.height(6.dp))
                Text(
                    item.fileName,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = scheme.onSurfaceVariant
                )
            }
        } else {
            AsyncImage(
                model = item.url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (item.type == MediaType.VIDEO) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }

        // badge tipe
        Box(
            Modifier.align(Alignment.TopEnd).padding(6.dp)
                .clip(CircleShape).background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                when (item.type) {
                    MediaType.IMAGE -> "IMG"
                    MediaType.VIDEO -> "VID"
                    MediaType.UNKNOWN -> "URL"
                },
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ───────────────────────── SINGLE VIEWER ─────────────────────────

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

    Column(Modifier.fillMaxSize()) {

        Box(
            Modifier.fillMaxWidth().weight(1f).background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            when (item.type) {
                MediaType.IMAGE -> AsyncImage(
                    model = item.url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                MediaType.VIDEO -> ExoVideoPlayer(item.url)

                MediaType.UNKNOWN -> Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = Color.White)
                    Text(item.url, color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // info + kontrol
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                item.url,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$index".let { "${index + 1} / $total" },
                    style = MaterialTheme.typography.labelLarge
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPrev) { Text("Prev") }
                    OutlinedButton(
                        onClick = {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                        }
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Buka")
                    }
                    OutlinedButton(onClick = onNext) { Text("Next") }
                    Button(onClick = { onDownload(item.url, item.type) }) {
                        Icon(Icons.Default.Download, contentDescription = null)
                    }
                }
            }
        }
    }
}

// ───────────────────────── EXOPLAYER ─────────────────────────

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
