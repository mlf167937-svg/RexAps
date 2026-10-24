// SsWebScreen.kt
package com.rexaps.rextools.ssweb

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rextools.downloader.ImageDownloader
import com.rexaps.rextools.ui.*
import kotlinx.coroutines.launch

@Composable
fun SsWebScreenPortrait(
    onBack: () -> Unit = {},
    vm: SsWebViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val toast = rememberRexToast()
    val s = MaterialTheme.colorScheme
    var saving by remember { mutableStateOf(false) }

    val phase = when {
        state.loading -> RexPhase.Loading
        state.result != null -> RexPhase.Result
        state.error != null -> RexPhase.Hidden
        else -> RexPhase.Idle
    }

    val presets = listOf(
        Triple("Mobile", "390", "844"),
        Triple("Tablet", "820", "1180"),
        Triple("Desktop", "1440", "900")
    )

    RexScreen(title = "Screenshot Web", onBack = onBack, toast = toast) {
        RexHero(
            icon = Icons.Default.CameraAlt,
            title = "Tangkap tampilan website",
            description = "Masukkan URL dan atur ukuran layar.",
            modifier = Modifier.entrance(0)
        )

        RexField(
            value = state.url,
            onChange = vm::onUrlChange,
            label = "URL (contoh: github.com)",
            icon = Icons.Default.Language,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = {
                focus.clearFocus()
                if (!state.loading) vm.capture()
            }),
            modifier = Modifier.entrance(1)
        )

        Column(Modifier.entrance(2), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RexOverline("Ukuran tampilan")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { (name, w, h) ->
                    RexFilterChip(
                        label = "$name  $w×$h",
                        selected = state.width.toString() == w && state.height.toString() == h
                    ) {
                        vm.onWidthChange(w)
                        vm.onHeightChange(h)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RexField(
                    value = state.width.toString(), onChange = vm::onWidthChange,
                    label = "Width", clearable = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
                RexField(
                    value = state.height.toString(), onChange = vm::onHeightChange,
                    label = "Height", clearable = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
                RexField(
                    value = state.scale.toString(), onChange = vm::onScaleChange,
                    label = "Scale", clearable = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        RexPrimaryButton(
            text = "Capture",
            icon = Icons.Default.CameraAlt,
            loading = state.loading,
            modifier = Modifier.entrance(3),
            onClick = {
                focus.clearFocus()
                vm.capture()
            }
        )

        RexErrorSlot(state.error, "Gagal mengambil screenshot")

        RexPhaseSwitch(
            phase = phase,
            idle = {
                RexEmptyState(
                    Icons.Default.CameraAlt, "Belum ada screenshot",
                    "Isi URL lalu tekan Capture untuk melihat hasilnya di sini."
                )
            },
            loading = { RexSkeletonPanel(media = true) },
            result = {
                state.result?.let { r ->
                    RexPanel(spacing = 14.dp, glow = true) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                r.title ?: "(no title)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                r.url ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = s.primary, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            r.description?.let {
                                Text(
                                    it, style = MaterialTheme.typography.bodySmall,
                                    color = s.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        val imgUrl = r.file_url ?: r.screenshot?.url
                        if (!imgUrl.isNullOrBlank()) {
                            BrowserFrame(r.url ?: imgUrl) {
                                RexImage(imgUrl, "Hasil screenshot website", Modifier.fillMaxWidth(), ContentScale.FillWidth)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                RexOutlinedButton(
                                    "Buka", Icons.Default.OpenInNew,
                                    {
                                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(imgUrl))) }
                                            .onFailure { toast.show("Tidak bisa membuka link", true) }
                                    },
                                    Modifier.weight(1f)
                                )
                                RexTonalButton(
                                    "Download", Icons.Default.Download,
                                    {
                                        if (!saving) {
                                            saving = true
                                            scope.launch {
                                                ImageDownloader.download(ctx, imgUrl)
                                                    .onSuccess { toast.show("Screenshot tersimpan") }
                                                    .onFailure { toast.show("Gagal: ${it.message}", true) }
                                                saving = false
                                            }
                                        }
                                    },
                                    Modifier.weight(1f), loading = saving
                                )
                            }
                        }
                    }
                }
            }
        )

        Spacer(Modifier.height(24.dp))
    }
}

/** Bingkai ala jendela browser supaya hasil screenshot terlihat seperti mockup. */
@Composable
internal fun BrowserFrame(url: String, content: @Composable () -> Unit) {
    val s = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RexMd)
            .background(s.surfaceContainerHighest)
            .border(hairline(), RexMd)
    ) {
        Row(
            Modifier.fillMaxWidth().background(s.surfaceContainerHigh).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(s.error, s.tertiary, s.primary).forEach { c ->
                Box(Modifier.size(9.dp).clip(CircleShape).background(c.copy(alpha = 0.85f)))
            }
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.weight(1f).clip(CircleShape).background(s.surface.copy(alpha = 0.7f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    url, style = MaterialTheme.typography.labelSmall,
                    color = s.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        content()
    }
}
