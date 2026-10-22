package com.rexaps.rextools.ssweb

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rexaps.rextools.downloader.ImageDownloader
import com.rexaps.rextools.ui.*
import kotlinx.coroutines.launch

@Composable
fun SsWebScreenLandscape(onBack: () -> Unit, vm: SsWebViewModel) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val toast = rememberRexToast()
    val colors = MaterialTheme.colorScheme
    var saving by remember { mutableStateOf(false) }
    val phase = when { state.loading -> RexPhase.Loading; state.result != null -> RexPhase.Result; state.error != null -> RexPhase.Hidden; else -> RexPhase.Idle }
    val presets = listOf(Triple("Mobile", "390", "844"), Triple("Tablet", "820", "1180"), Triple("Desktop", "1440", "900"))

    RexScreen(title = "Screenshot Web", onBack = onBack, toast = toast, scroll = false, padded = false) {
        Row(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(Modifier.weight(0.42f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                RexHero(Icons.Default.CameraAlt, "Tangkap tampilan website", "Atur ukuran viewport lalu ambil screenshot.")
                RexField(
                    value = state.url, onChange = vm::onUrlChange, label = "URL website", icon = Icons.Default.Language,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { focus.clearFocus(); if (!state.loading) vm.capture() })
                )
                RexOverline("Preset viewport")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { (name, w, h) ->
                        RexFilterChip("$name  $w×$h", state.width.toString() == w && state.height.toString() == h) {
                            vm.onWidthChange(w); vm.onHeightChange(h)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RexField(state.width.toString(), vm::onWidthChange, "Width", clearable = false, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    RexField(state.height.toString(), vm::onHeightChange, "Height", clearable = false, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    RexField(state.scale.toString(), vm::onScaleChange, "Scale", clearable = false, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                RexPrimaryButton("Capture", Icons.Default.CameraAlt, { focus.clearFocus(); vm.capture() }, loading = state.loading)
                RexErrorSlot(state.error, "Gagal mengambil screenshot")
            }
            Column(Modifier.weight(0.58f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RexOverline("HASIL SCREENSHOT")
                RexPhaseSwitch(
                    phase = phase,
                    idle = { RexEmptyState(Icons.Default.CameraAlt, "Belum ada screenshot", "Hasil capture akan tampil di panel ini.") },
                    loading = { RexSkeletonPanel(media = true) },
                    result = {
                        state.result?.let { r ->
                            RexPanel(spacing = 14.dp, glow = true) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(r.title ?: "(no title)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(r.url.orEmpty(), style = MaterialTheme.typography.bodySmall, color = colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    r.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis) }
                                }
                                val imageUrl = r.file_url ?: r.screenshot?.url
                                if (!imageUrl.isNullOrBlank()) {
                                    BrowserFrame(r.url ?: imageUrl) { RexImage(imageUrl, "Hasil screenshot website", Modifier.fillMaxWidth(), ContentScale.FillWidth) }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        RexOutlinedButton("Buka", Icons.Default.OpenInNew, {
                                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl))) }.onFailure { toast.show("Tidak bisa membuka link", true) }
                                        }, Modifier.weight(1f))
                                        RexTonalButton("Download", Icons.Default.Download, {
                                            if (!saving) { saving = true; scope.launch { ImageDownloader.download(ctx, imageUrl).onSuccess { toast.show("Screenshot tersimpan") }.onFailure { toast.show("Gagal: ${it.message}", true) }; saving = false } }
                                        }, Modifier.weight(1f), loading = saving)
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
