// PinterestScreen.kt
package com.rexaps.rextools.search

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.rexaps.rextools.downloader.ImageDownloader
import com.rexaps.rextools.ui.*
import kotlinx.coroutines.launch

@Composable
fun PinterestScreen(
    onBack: () -> Unit = {},
    vm: PinterestViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }

    RexScreen(title = "Pinterest", onBack = onBack, scroll = false) {
        RexField(
            value = query,
            onChange = { query = it },
            label = "Cari gambar Pinterest",
            icon = Icons.Default.Search,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                if (!state.loading) vm.search(query)
            })
        )

        RexPrimaryButton(
            text = "Cari",
            icon = Icons.Default.Search,
            loading = state.loading,
            loadingText = "Mencari…",
            onClick = {
                focusManager.clearFocus()
                vm.search(query)
            }
        )

        RexErrorSlot(state.error, "Gagal memuat gambar")

        if (state.images.isNotEmpty()) {
            val currentUrl = state.images[state.currentIndex]
            val total = state.images.size
            var drag by remember { mutableFloatStateOf(0f) }

            RexPanel(
                modifier = Modifier.weight(1f),
                padding = 0.dp,
                spacing = 0.dp
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (drag > 80f) vm.prev() else if (drag < -80f) vm.next()
                                    drag = 0f
                                },
                                onDragCancel = { drag = 0f }
                            ) { _, delta -> drag += delta }
                        }
                ) {
                    Crossfade(targetState = currentUrl, label = "pin") { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Gambar ${state.currentIndex + 1} dari $total",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)
                    ) {
                        Text(
                            "${state.currentIndex + 1} / $total",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalIconButton(onClick = { vm.prev() }, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Gambar sebelumnya")
                }
                RexPrimaryButton(
                    text = "Download",
                    icon = Icons.Default.Download,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            ImageDownloader.download(context, currentUrl)
                                .onSuccess { Toast.makeText(context, "Tersimpan", Toast.LENGTH_SHORT).show() }
                                .onFailure { Toast.makeText(context, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show() }
                        }
                    }
                )
                FilledTonalIconButton(onClick = { vm.next() }, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Gambar berikutnya")
                }
            }
        } else if (!state.loading) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                RexEmptyState(
                    icon = Icons.Default.Image,
                    title = "Belum ada gambar",
                    message = "Ketik kata kunci lalu tekan Cari, hasilnya muncul di sini."
                )
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}