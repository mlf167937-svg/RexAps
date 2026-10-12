// TiktokDonwloaderScreen.kt
package com.rexaps.rextools.tiktok

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.rexaps.rextools.downloader.MediaDownloader
import com.rexaps.rextools.ui.*
import kotlinx.coroutines.launch

@Composable
fun TiktokDownloaderScreen(
    onBack: () -> Unit = {},
    vm: TiktokDownloaderViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    RexScreen(title = "TikTok Downloader", onBack = onBack) {
        RexHero(
            icon = Icons.Default.Download,
            title = "Unduh video & foto",
            description = "Tempel link untuk mengambil video, foto, atau musiknya."
        )

        RexField(
            value = state.url,
            onChange = vm::onUrlChange,
            label = "URL TikTok",
            placeholder = "https://vt.tiktok.com/xxx/",
            icon = Icons.Default.Link,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = {
                focusManager.clearFocus()
                if (!state.loading) vm.download()
            })
        )

        RexPrimaryButton(
            text = "Ambil Media",
            icon = Icons.Default.Download,
            loading = state.loading,
            onClick = {
                focusManager.clearFocus()
                vm.download()
            }
        )

        RexErrorSlot(state.error, "Gagal mengambil media")

        state.result?.let { r ->
            ResultCard(
                result = r,
                onOpen = { url -> ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                onDownloadVideo = { url ->
                    scope.launch {
                        MediaDownloader.downloadVideo(ctx, url)
                            .onSuccess { Toast.makeText(ctx, "Video tersimpan", Toast.LENGTH_SHORT).show() }
                            .onFailure { Toast.makeText(ctx, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show() }
                    }
                },
                onDownloadImage = { url ->
                    scope.launch {
                        MediaDownloader.downloadImage(ctx, url)
                            .onSuccess { Toast.makeText(ctx, "Gambar tersimpan", Toast.LENGTH_SHORT).show() }
                            .onFailure { Toast.makeText(ctx, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show() }
                    }
                },
                onDownloadMusic = { url ->
                    scope.launch {
                        MediaDownloader.downloadVideo(ctx, url, "tiktok_music_${System.currentTimeMillis()}.mp4")
                            .onSuccess { Toast.makeText(ctx, "Musik tersimpan", Toast.LENGTH_SHORT).show() }
                            .onFailure { Toast.makeText(ctx, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show() }
                    }
                }
            )
        }

        if (state.result == null && state.error == null && !state.loading) {
            RexEmptyState(
                icon = Icons.Default.Link,
                title = "Belum ada media",
                message = "Tempel link TikTok lalu tekan Ambil Media."
            )
        }

        Spacer(Modifier.height(24.dp))
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
    val s = MaterialTheme.colorScheme

    RexPanel {
        result.author?.let { a ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(
                    model = a.avatar,
                    contentDescription = a.nickname?.let { "Foto profil $it" } ?: "Foto profil",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, s.primary.copy(alpha = 0.6f), CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(s.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        a.nickname ?: "-",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "@${a.fullname ?: "-"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = s.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        result.title?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }

        result.cover?.let { cover ->
            MediaFrame {
                AsyncImage(
                    model = cover,
                    contentDescription = "Sampul konten",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        if (result.isVideo && !result.videoUrl.isNullOrBlank()) {
            val videoUrl = result.videoUrl.orEmpty()
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                RexOutlinedButton("Putar", Icons.Default.PlayArrow, { onOpen(videoUrl) }, Modifier.weight(1f))
                RexTonalButton("Simpan Video", Icons.Default.Download, { onDownloadVideo(videoUrl) }, Modifier.weight(1.3f))
            }
        }

        if (!result.isVideo && result.photoUrls.isNotEmpty()) {
            val photos = result.photoUrls
            RexOverline("Foto (${photos.size})", Modifier.semantics { heading() })
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                photos.forEachIndexed { i, imgUrl ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MediaFrame {
                            AsyncImage(
                                model = imgUrl,
                                contentDescription = "Foto ${i + 1} dari ${photos.size}",
                                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        RexTonalButton(
                            "Simpan foto ${i + 1}",
                            Icons.Default.Download,
                            { onDownloadImage(imgUrl) },
                            Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        result.music_info?.let { m ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RexMd)
                    .background(s.surfaceContainerHigh.copy(alpha = 0.7f))
                    .border(1.dp, s.outlineVariant.copy(alpha = 0.4f), RexMd)
                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RexIconTile(Icons.Default.MusicNote, size = 42.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        m.title ?: "-",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        m.author ?: "-",
                        style = MaterialTheme.typography.labelMedium,
                        color = s.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                m.url?.takeIf { it.isNotBlank() }?.let { url ->
                    IconButton(onClick = { onDownloadMusic(url) }) {
                        Icon(Icons.Default.Download, contentDescription = "Simpan musik")
                    }
                }
            }
        }

        result.stats?.let { st ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                RexStat("Views", st.views ?: "-", Modifier.weight(1f), Icons.Default.Visibility)
                RexStat("Likes", st.likes ?: "-", Modifier.weight(1f), Icons.Default.FavoriteBorder)
                RexStat("Comment", st.comment ?: "-", Modifier.weight(1f), Icons.Default.ChatBubbleOutline)
                RexStat("Share", st.share ?: "-", Modifier.weight(1f), Icons.Default.Share)
            }
        }
    }
}

@Composable
private fun MediaFrame(content: @Composable () -> Unit) {
    val s = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RexMd)
            .background(s.surfaceContainerHighest)
            .border(1.dp, s.outlineVariant.copy(alpha = 0.6f), RexMd),
        contentAlignment = Alignment.Center
    ) { content() }
}