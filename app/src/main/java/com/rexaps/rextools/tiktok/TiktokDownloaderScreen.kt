package com.rexaps.rextools.tiktok

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.rexaps.rextools.downloader.MediaDownloader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiktokDownloaderScreen(
    onBack: () -> Unit = {},
    vm: TiktokDownloaderViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TikTok Downloader") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.url,
                onValueChange = vm::onUrlChange,
                label = { Text("URL TikTok") },
                placeholder = { Text("https://vt.tiktok.com/xxx/") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { vm.download() },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.loading) "Loading..." else "Ambil Media")
            }

            state.error?.let {
                Text("Error: $it", color = MaterialTheme.colorScheme.error)
            }

            state.result?.let { r ->
                ResultCard(
                    result = r,
                    onOpen = { url ->
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    onDownloadVideo = { url ->
                        scope.launch {
                            MediaDownloader.downloadVideo(ctx, url)
                                .onSuccess { Toast.makeText(ctx, "video tersimpan", Toast.LENGTH_SHORT).show() }
                                .onFailure { Toast.makeText(ctx, "gagal: ${it.message}", Toast.LENGTH_SHORT).show() }
                        }
                    },
                    onDownloadImage = { url ->
                        scope.launch {
                            MediaDownloader.downloadImage(ctx, url)
                                .onSuccess { Toast.makeText(ctx, "gambar tersimpan", Toast.LENGTH_SHORT).show() }
                                .onFailure { Toast.makeText(ctx, "gagal: ${it.message}", Toast.LENGTH_SHORT).show() }
                        }
                    },
                    onDownloadMusic = { url ->
                        scope.launch {
                            MediaDownloader.downloadVideo(ctx, url, "tiktok_music_${System.currentTimeMillis()}.mp4")
                                .onSuccess { Toast.makeText(ctx, "musik tersimpan", Toast.LENGTH_SHORT).show() }
                                .onFailure { Toast.makeText(ctx, "gagal: ${it.message}", Toast.LENGTH_SHORT).show() }
                        }
                    }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ResultCard(
    result: com.rexaps.rextools.TiktokDownloadResult,
    onOpen: (String) -> Unit,
    onDownloadVideo: (String) -> Unit,
    onDownloadImage: (String) -> Unit,
    onDownloadMusic: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val mediaList = result.data.orEmpty()

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Author
            result.author?.let { a ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = a.avatar,
                        contentDescription = a.nickname,
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(scheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            a.nickname ?: "-",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "@${a.fullname ?: "-"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Title
            result.title?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
            }

            // Cover
            result.cover?.let { cover ->
                AsyncImage(
                    model = cover,
                    contentDescription = "cover",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            // Video case
            if (result.isVideo && mediaList.isNotEmpty()) {
                val videoUrl = mediaList[0]
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { onOpen(videoUrl) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Putar")
                    }
                    Button(
                        onClick = { onDownloadVideo(videoUrl) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Video")
                    }
                }
            }

            // Photo case
            if (!result.isVideo && mediaList.isNotEmpty()) {
                Text(
                    "Foto (${mediaList.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    mediaList.forEachIndexed { i, imgUrl ->
                        Card(shape = RoundedCornerShape(12.dp)) {
                            Column {
                                AsyncImage(
                                    model = imgUrl,
                                    contentDescription = "foto $i",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 320.dp)
                                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                    contentScale = ContentScale.Fit
                                )
                                Button(
                                    onClick = { onDownloadImage(imgUrl) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Simpan foto ${i + 1}")
                                }
                            }
                        }
                    }
                }
            }

            // Music
            result.music_info?.let { m ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = scheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                m.title ?: "-",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                m.author ?: "-",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        m.url?.takeIf { it.isNotBlank() }?.let { url ->
                            IconButton(onClick = { onDownloadMusic(url) }) {
                                Icon(Icons.Default.Download, contentDescription = "simpan musik")
                            }
                        }
                    }
                }
            }

            // Stats
            result.stats?.let { s ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatBox("Views", s.views ?: "-")
                    StatBox("Likes", s.likes ?: "-")
                    StatBox("Comment", s.comment ?: "-")
                    StatBox("Share", s.share ?: "-")
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
