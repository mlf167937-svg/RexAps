// TiktokDonwloaderScreen.kt
package com.rexaps.rextools.tiktok

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
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
    val focus = LocalFocusManager.current
    val clipboard = LocalClipboardManager.current
    val toast = rememberRexToast()
    var busy by remember { mutableStateOf<String?>(null) }

    fun runDownload(key: String, okMsg: String, block: suspend () -> Result<*>) {
        if (busy != null) return
        busy = key
        scope.launch {
            block()
                .onSuccess { toast.show(okMsg) }
                .onFailure { toast.show("Gagal: ${it.message}", true) }
            busy = null
        }
    }

    val phase = when {
        state.loading -> RexPhase.Loading
        state.result != null -> RexPhase.Result
        state.error != null -> RexPhase.Hidden
        else -> RexPhase.Idle
    }

    RexScreen(title = "TikTok Downloader", onBack = onBack, toast = toast) {
        RexHero(
            icon = Icons.Default.Download,
            title = "Unduh video & foto",
            description = "Tempel link untuk mengambil video, foto, atau musiknya.",
            modifier = Modifier.entrance(0)
        )

        Column(Modifier.entrance(1), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RexField(
                value = state.url,
                onChange = vm::onUrlChange,
                label = "URL TikTok",
                placeholder = "https://vt.tiktok.com/xxx/",
                icon = Icons.Default.Link,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    focus.clearFocus()
                    if (!state.loading) vm.download()
                })
            )
            RexQuickAction("Tempel link", Icons.Default.ContentPaste, {
                clipboard.getText()?.text?.trim()?.takeIf { it.isNotEmpty() }?.let(vm::onUrlChange)
            })
        }

        RexPrimaryButton(
            text = "Ambil Media",
            icon = Icons.Default.Download,
            loading = state.loading,
            modifier = Modifier.entrance(2),
            onClick = {
                focus.clearFocus()
                vm.download()
            }
        )

        RexErrorSlot(state.error, "Gagal mengambil media")

        RexPhaseSwitch(
            phase = phase,
            idle = {
                RexEmptyState(Icons.Default.Link, "Belum ada media", "Tempel link TikTok lalu tekan Ambil Media.")
            },
            loading = { RexSkeletonPanel(media = true) },
            result = {
                state.result?.let { r ->
                    ResultCard(
                        result = r,
                        busy = busy,
                        onOpen = { url ->
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                .onFailure { toast.show("Tidak bisa membuka link", true) }
                        },
                        onDownloadVideo = { url -> runDownload("video", "Video tersimpan") { MediaDownloader.downloadVideo(ctx, url) } },
                        onDownloadImage = { url -> runDownload("img:$url", "Gambar tersimpan") { MediaDownloader.downloadImage(ctx, url) } },
                        onDownloadMusic = { url ->
                            runDownload("music", "Musik tersimpan") {
                                MediaDownloader.downloadVideo(ctx, url, "tiktok_music_${System.currentTimeMillis()}.mp4")
                            }
                        }
                    )
                }
            }
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ResultCard(
    result: com.rexaps.rextools.TiktokDownloadResult,
    busy: String?,
    onOpen: (String) -> Unit,
    onDownloadVideo: (String) -> Unit,
    onDownloadImage: (String) -> Unit,
    onDownloadMusic: (String) -> Unit
) {
    val s = MaterialTheme.colorScheme

    RexPanel(glow = true) {
        result.author?.let { a ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RexAvatar(
                    model = a.avatar,
                    contentDescription = a.nickname?.let { "Foto profil $it" } ?: "Foto profil",
                    size = 50.dp, ring = 2.dp
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        a.nickname ?: "-",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "@${a.fullname ?: "-"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = s.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        result.title?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }

        result.cover?.let { cover ->
            MediaFrame {
                RexImage(
                    cover, "Sampul konten",
                    Modifier.fillMaxWidth().heightIn(max = 340.dp),
                    ContentScale.Fit
                )
                if (result.isVideo) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }

        if (result.isVideo && !result.videoUrl.isNullOrBlank()) {
            val videoUrl = result.videoUrl.orEmpty()
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                RexOutlinedButton("Putar", Icons.Default.PlayArrow, { onOpen(videoUrl) }, Modifier.weight(1f))
                RexTonalButton(
                    "Simpan Video", Icons.Default.Download, { onDownloadVideo(videoUrl) },
                    Modifier.weight(1.3f), enabled = busy == null || busy == "video", loading = busy == "video"
                )
            }
        }

        if (!result.isVideo && result.photoUrls.isNotEmpty()) {
            val photos = result.photoUrls
            RexOverline("Foto (${photos.size})", Modifier.semantics { heading() })
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                photos.forEachIndexed { i, imgUrl ->
                    Column(
                        Modifier.entrance(i.coerceAtMost(5), 90),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MediaFrame {
                            RexImage(
                                imgUrl, "Foto ${i + 1} dari ${photos.size}",
                                Modifier.fillMaxWidth().heightIn(max = 340.dp),
                                ContentScale.Fit
                            )
                        }
                        RexTonalButton(
                            "Simpan foto ${i + 1}", Icons.Default.Download, { onDownloadImage(imgUrl) },
                            Modifier.fillMaxWidth(),
                            enabled = busy == null || busy == "img:$imgUrl", loading = busy == "img:$imgUrl"
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
                    .background(Brush.horizontalGradient(listOf(s.primary.copy(alpha = 0.10f), s.surfaceContainerHigh.copy(alpha = 0.7f))))
                    .border(1.dp, s.outlineVariant.copy(alpha = 0.5f), RexMd)
                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RexIconTile(Icons.Default.MusicNote, size = 44.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        m.title ?: "-",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        m.author ?: "-",
                        style = MaterialTheme.typography.labelMedium,
                        color = s.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                RexEqualizer()
                m.url?.takeIf { it.isNotBlank() }?.let { url ->
                    if (busy == "music") {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        IconButton(onClick = { onDownloadMusic(url) }, enabled = busy == null) {
                            Icon(Icons.Default.Download, contentDescription = "Simpan musik", tint = s.primary)
                        }
                    }
                }
            }
        }

        result.stats?.let { st ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                RexStat("Views", st.views ?: "-", Modifier.weight(1f), Icons.Default.Visibility, 0)
                RexStat("Likes", st.likes ?: "-", Modifier.weight(1f), Icons.Default.FavoriteBorder, 1)
                RexStat("Comment", st.comment ?: "-", Modifier.weight(1f), Icons.Default.ChatBubbleOutline, 2)
                RexStat("Share", st.share ?: "-", Modifier.weight(1f), Icons.Default.Share, 3)
            }
        }
    }
}

@Composable
private fun MediaFrame(content: @Composable BoxScope.() -> Unit) {
    val s = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RexMd)
            .background(s.surfaceContainerHighest)
            .border(1.dp, s.outlineVariant.copy(alpha = 0.6f), RexMd),
        contentAlignment = Alignment.Center,
        content = content
    )
}