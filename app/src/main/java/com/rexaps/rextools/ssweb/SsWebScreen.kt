// SsWebScreen.kt
package com.rexaps.rextools.ssweb

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import coil.compose.AsyncImage
import com.rexaps.rextools.downloader.ImageDownloader
import com.rexaps.rextools.ui.*
import kotlinx.coroutines.launch

@Composable
fun SsWebScreen(
    onBack: () -> Unit = {},
    vm: SsWebViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val s = MaterialTheme.colorScheme

    RexScreen(title = "Screenshot Web", onBack = onBack) {
        RexHero(
            icon = Icons.Default.CameraAlt,
            title = "Tangkap tampilan website",
            description = "Masukkan URL dan atur ukuran layar."
        )

        RexField(
            value = state.url,
            onChange = vm::onUrlChange,
            label = "URL (contoh: github.com)",
            icon = Icons.Default.Language,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = {
                focusManager.clearFocus()
                if (!state.loading) vm.capture()
            })
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RexOverline("Ukuran tampilan")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RexField(
                    value = state.width.toString(),
                    onChange = vm::onWidthChange,
                    label = "Width",
                    clearable = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
                RexField(
                    value = state.height.toString(),
                    onChange = vm::onHeightChange,
                    label = "Height",
                    clearable = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
                RexField(
                    value = state.scale.toString(),
                    onChange = vm::onScaleChange,
                    label = "Scale",
                    clearable = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        RexPrimaryButton(
            text = "Capture",
            icon = Icons.Default.CameraAlt,
            loading = state.loading,
            onClick = {
                focusManager.clearFocus()
                vm.capture()
            }
        )

        RexErrorSlot(state.error, "Gagal mengambil screenshot")

        state.result?.let { r ->
            RexPanel(spacing = 14.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        r.title ?: "(no title)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        r.url ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = s.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    r.description?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = s.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val imgUrl = r.file_url ?: r.screenshot?.url
                if (!imgUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imgUrl,
                        contentDescription = "Hasil screenshot website",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RexMd)
                            .background(s.surfaceVariant)
                            .border(1.dp, s.outlineVariant.copy(alpha = 0.6f), RexMd),
                        contentScale = ContentScale.FillWidth
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        RexOutlinedButton(
                            "Buka", Icons.Default.OpenInNew,
                            { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(imgUrl))) },
                            Modifier.weight(1f)
                        )
                        RexTonalButton(
                            "Download", Icons.Default.Download,
                            {
                                scope.launch {
                                    ImageDownloader.download(ctx, imgUrl)
                                        .onSuccess { Toast.makeText(ctx, "Tersimpan", Toast.LENGTH_SHORT).show() }
                                        .onFailure { Toast.makeText(ctx, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show() }
                                }
                            },
                            Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (state.result == null && state.error == null && !state.loading) {
            RexEmptyState(
                icon = Icons.Default.CameraAlt,
                title = "Belum ada screenshot",
                message = "Isi URL lalu tekan Capture untuk melihat hasilnya di sini."
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}