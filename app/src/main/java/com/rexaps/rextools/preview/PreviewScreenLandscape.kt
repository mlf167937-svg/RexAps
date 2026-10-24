package com.rexaps.rextools.preview

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import com.rexaps.rextools.downloader.MediaDownloader
import com.rexaps.rextools.ui.*
import kotlinx.coroutines.launch

@Composable
fun PreviewScreenLandscape(onBack: () -> Unit, vm: PreviewViewModel) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val toast = rememberRexToast()
    var saving by remember { mutableStateOf(false) }
    val goBack: () -> Unit = { when (state.mode) { PreviewMode.INPUT -> onBack(); PreviewMode.GRID -> vm.backToInput(); PreviewMode.SINGLE -> vm.backToGrid() } }
    BackHandler(enabled = state.mode != PreviewMode.INPUT) { goBack() }
    val title = when (state.mode) { PreviewMode.INPUT -> "Media Preview"; PreviewMode.GRID -> "Hasil Ekstraksi"; PreviewMode.SINGLE -> "Pratinjau" }
    val subtitle = when (state.mode) { PreviewMode.INPUT -> "Tempel link, lihat medianya"; PreviewMode.GRID -> "${state.items.size} item ditemukan"; PreviewMode.SINGLE -> "Media ${state.currentIndex + 1} dari ${state.items.size}" }

    RexScreen(title = title, subtitle = subtitle, onBack = goBack, scroll = false, padded = false, toast = toast) {
        AnimatedContent(targetState = state.mode, modifier = Modifier.fillMaxSize(), transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.98f)).togetherWith(fadeOut() + scaleOut(targetScale = 0.98f)) }, label = "preview-landscape-mode") { mode ->
            when (mode) {
                PreviewMode.INPUT -> {
                    val links = remember(state.rawInput) { Regex("""https?://[^\s"'<>\\]+""").findAll(state.rawInput).count() }
                    Row(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Column(Modifier.weight(1.1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            RexHero(Icons.Default.Link, "Ekstrak media dari mana saja", "URL tunggal, JSON, atau teks apa pun yang berisi link.")
                            RexPanel(modifier = Modifier.weight(1f).fillMaxWidth(), padding = 12.dp, spacing = 10.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically) { RexOverline("INPUT"); Spacer(Modifier.width(10.dp)); if (links > 0) RexPill("$links link terdeteksi", icon = Icons.Default.Link); Spacer(Modifier.weight(1f)); TextButton(onClick = { clipboard.getText()?.text?.takeIf { it.isNotBlank() }?.let(vm::onInputChange) }) { Icon(Icons.Default.ContentPaste, null); Spacer(Modifier.width(4.dp)); Text("Tempel") } }
                                OutlinedTextField(
                                    value = state.rawInput, onValueChange = vm::onInputChange,
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    placeholder = { Text("https://... atau JSON berisi URL", fontFamily = FontFamily.Monospace) },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                    minLines = 5, maxLines = 12,
                                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                )
                                RexErrorSlot(state.error, "Input belum dapat diproses")
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    RexOutlinedButton("Bersihkan", Icons.Default.DeleteOutline, vm::clear, Modifier.weight(1f))
                                    RexPrimaryButton("Ekstrak media", Icons.Default.Link, vm::extract, Modifier.weight(1.25f))
                                }
                            }
                        }
                        Column(Modifier.weight(0.78f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.Start) {
                            RexPanel(spacing = 14.dp) {
                                RexOverline("CARA KERJA")
                                Text("Ambil URL dari teks", style = MaterialTheme.typography.titleLarge)
                                Text("Tempel satu URL, beberapa URL, atau teks/JSON yang memuat tautan gambar dan video. Link yang dikenali akan dikelompokkan menjadi grid pratinjau.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 21.sp)
                                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    RexIconTile(Icons.Default.Link, size = 48.dp)
                                    Column { Text("Tipe input", style = MaterialTheme.typography.titleSmall); Text("URL · JSON · teks campuran", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    RexIconTile(Icons.Default.OpenInNew, size = 48.dp)
                                    Column { Text("Pratinjau", style = MaterialTheme.typography.titleSmall); Text("Buka satu media atau lihat semua hasil di grid.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                            }
                        }
                    }
                }
                PreviewMode.GRID -> GridView(items = state.items, onOpen = { vm.openSingle(it) })
                PreviewMode.SINGLE -> {
                    val item = state.currentItem
                    if (item != null) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(Modifier.weight(1f).fillMaxHeight().clip(RexLg).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                                when (item.type) {
                                    MediaType.IMAGE -> ZoomableImage(item.url, "Pratinjau media ${state.currentIndex + 1}")
                                    MediaType.VIDEO -> ExoVideoPlayer(item.url)
                                    MediaType.UNKNOWN -> Text("Format tidak dikenali", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Column(Modifier.widthIn(min = 280.dp, max = 360.dp).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                RexOverline("MEDIA ${state.currentIndex + 1} / ${state.items.size}")
                                Text(item.fileName, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                RexPanel(padding = 10.dp, spacing = 8.dp) {
                                    Text(item.url, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                    RexOutlinedButton("Salin link", Icons.Default.ContentPaste, { clipboard.setText(AnnotatedString(item.url)); toast.show("Link disalin") }, Modifier.fillMaxWidth())
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    RexRoundButton(Icons.Default.ChevronLeft, "Sebelumnya", vm::prev, size = 44.dp)
                                    RexRoundButton(Icons.Default.ChevronRight, "Berikutnya", vm::next, size = 44.dp)
                                }
                                RexOutlinedButton("Buka di browser", Icons.Default.OpenInNew, { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url))) }.onFailure { toast.show("Tidak bisa membuka link", true) } }, Modifier.fillMaxWidth())
                                RexPrimaryButton("Simpan media", Icons.Default.Download, {
                                    if (!saving) { saving = true; scope.launch { val res = if (item.type == MediaType.VIDEO) MediaDownloader.downloadVideo(ctx, item.url) else MediaDownloader.downloadImage(ctx, item.url); res.onSuccess { toast.show("Tersimpan ke perangkat") }.onFailure { toast.show("Gagal menyimpan: ${it.message}", true) }; saving = false } }
                                }, Modifier.fillMaxWidth(), loading = saving, loadingText = "Menyimpan…")
                            }
                        }
                    }
                }
            }
        }
    }
}
