package com.rexaps.rextools.tiktok

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rexaps.rextools.downloader.MediaDownloader
import com.rexaps.rextools.ui.*
import kotlinx.coroutines.launch

@Composable
fun TiktokDownloaderScreenLandscape(onBack: () -> Unit, vm: TiktokDownloaderViewModel) {
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
        scope.launch { block().onSuccess { toast.show(okMsg) }.onFailure { toast.show("Gagal: ${it.message}", true) }; busy = null }
    }
    val phase = when { state.loading -> RexPhase.Loading; state.result != null -> RexPhase.Result; state.error != null -> RexPhase.Hidden; else -> RexPhase.Idle }

    RexScreen(title = "TikTok Downloader", onBack = onBack, toast = toast, scroll = false, padded = false) {
        Row(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.weight(0.40f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                RexHero(Icons.Default.Download, "Unduh video & foto", "Tempel tautan TikTok untuk melihat video, foto, atau audio.")
                RexField(
                    state.url, vm::onUrlChange, "URL TikTok", placeholder = "https://vt.tiktok.com/xxx/", icon = Icons.Default.Link,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { focus.clearFocus(); if (!state.loading) vm.download() })
                )
                RexTonalButton("Tempel link", Icons.Default.ContentPaste, { clipboard.getText()?.text?.trim()?.takeIf { it.isNotEmpty() }?.let(vm::onUrlChange) }, Modifier.fillMaxWidth())
                RexPrimaryButton("Ambil Media", Icons.Default.Download, { focus.clearFocus(); vm.download() }, loading = state.loading)
                RexErrorSlot(state.error, "Gagal mengambil media")
            }
            Column(Modifier.weight(0.60f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RexOverline("MEDIA & AKSI")
                RexPhaseSwitch(
                    phase = phase,
                    idle = { RexEmptyState(Icons.Default.Link, "Belum ada media", "Hasil video atau foto akan muncul di sini.") },
                    loading = { RexSkeletonPanel(media = true) },
                    result = {
                        state.result?.let { r ->
                            ResultCard(
                                result = r, busy = busy,
                                onOpen = { url -> runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.onFailure { toast.show("Tidak bisa membuka link", true) } },
                                onDownloadVideo = { url -> runDownload("video", "Video tersimpan") { MediaDownloader.downloadVideo(ctx, url) } },
                                onDownloadImage = { url -> runDownload("img:$url", "Gambar tersimpan") { MediaDownloader.downloadImage(ctx, url) } },
                                onDownloadMusic = { url -> runDownload("music", "Musik tersimpan") { MediaDownloader.downloadVideo(ctx, url, "tiktok_music_${System.currentTimeMillis()}.mp4") } }
                            )
                        }
                    }
                )
            }
        }
    }
}
