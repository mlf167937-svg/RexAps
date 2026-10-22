package com.rexaps.rextools.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.rexaps.rextools.downloader.ImageDownloader
import com.rexaps.rextools.ui.*
import kotlinx.coroutines.launch

@Composable
fun PinterestScreenLandscape(onBack: () -> Unit, vm: PinterestViewModel) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val toast = rememberRexToast()
    var query by rememberSaveable { mutableStateOf(state.query) }
    var saving by remember { mutableStateOf(false) }
    val phase = when { state.loading -> RexPhase.Loading; state.images.isNotEmpty() -> RexPhase.Result; state.error != null -> RexPhase.Hidden; else -> RexPhase.Idle }

    RexScreen(title = "Pinterest", onBack = onBack, scroll = false, padded = false, toast = toast) {
        Row(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.width(300.dp).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                RexHero(Icons.Default.Image, "Pinterest Search", "Cari gambar dengan pratinjau satu per satu.")
                RexField(query, { query = it }, "Kata kunci gambar", icon = Icons.Default.Search, shape = CircleShape,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focus.clearFocus(); if (!state.loading) vm.search(query) }))
                RexPrimaryButton("Cari gambar", Icons.Default.Search, { focus.clearFocus(); vm.search(query) }, loading = state.loading, loadingText = "Mencari…")
                RexErrorSlot(state.error, "Gagal memuat gambar")
                if (state.images.isNotEmpty()) RexPill("${state.currentIndex + 1} dari ${state.images.size} gambar", icon = Icons.Default.Image)
                RexPanel(spacing = 8.dp) {
                    RexOverline("TIPS")
                    Text("Geser gambar ke kiri/kanan atau gunakan kontrol untuk berpindah. Unduh gambar yang sedang aktif.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight().clip(RexLg).background(MaterialTheme.colorScheme.surfaceContainerLow), contentAlignment = Alignment.Center) {
                RexPhaseSwitch(
                    phase = phase, modifier = Modifier.fillMaxSize(),
                    idle = { RexEmptyState(Icons.Default.Image, "Belum ada gambar", "Ketik kata kunci lalu tekan Cari.") },
                    loading = { Box(Modifier.fillMaxSize().clip(RexLg).background(MaterialTheme.colorScheme.surfaceContainerHigh).shimmer()) },
                    result = {
                        if (state.images.isNotEmpty()) Deck(
                            images = state.images, index = state.currentIndex, saving = saving,
                            onPrev = vm::prev, onNext = vm::next,
                            onSave = { url -> if (!saving) { saving = true; scope.launch { ImageDownloader.download(context, url).onSuccess { toast.show("Gambar tersimpan") }.onFailure { toast.show("Gagal: ${it.message}", true) }; saving = false } } }
                        )
                    }
                )
            }
        }
    }
}
