package com.rexaps.rextools.tiktok

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.rexaps.rextools.ui.*
import kotlinx.coroutines.delay

@Composable
fun TiktokStalkerScreenLandscape(onBack: () -> Unit, vm: TiktokStalkerViewModel) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val focus = LocalFocusManager.current
    val clipboard = LocalClipboardManager.current
    val toast = rememberRexToast()
    var fullMode by rememberSaveable { mutableStateOf(false) }
    var scrapeUrl by remember { mutableStateOf<String?>(null) }
    var scrapeError by remember { mutableStateOf<String?>(null) }
    val scraping = scrapeUrl != null
    val busy = state.loading || scraping
    val error = scrapeError ?: state.error
    val hasResult = state.result != null || state.fullResult != null
    val phase = when { busy -> RexPhase.Loading; hasResult -> RexPhase.Result; error != null -> RexPhase.Hidden; else -> RexPhase.Idle }

    LaunchedEffect(scrapeUrl) {
        if (scrapeUrl != null) { delay(SCRAPE_TIMEOUT_MS); scrapeError = "Waktu habis. Coba lagi beberapa saat."; scrapeUrl = null }
    }
    var seenFull by remember { mutableStateOf(state.fullResult != null) }
    LaunchedEffect(state.fullResult) { if (state.fullResult != null && !seenFull) toast.show("Profil lengkap dimuat"); seenFull = state.fullResult != null }
    fun openUrl(url: String) {
        if (!url.startsWith("http")) { toast.show("Link tidak tersedia", true); return }
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.onFailure { toast.show("Tidak bisa membuka link", true) }
    }
    fun start() {
        focus.clearFocus()
        if (busy) return
        scrapeError = null
        val username = state.username.trim().removePrefix("@")
        if (fullMode) { if (username.isBlank()) scrapeError = "Isi username dulu." else scrapeUrl = "https://www.tiktok.com/@$username" }
        else vm.stalkBasic()
    }

    Box(Modifier.fillMaxSize()) {
        RexScreen(title = "TikTok Stalker", onBack = onBack, toast = toast, scroll = false, padded = false) {
            Row(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(0.38f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    RexHero(Icons.Default.Person, "Cari profil TikTok", "Profil publik, statistik, story, dan video.")
                    RexField(
                        state.username, vm::onUsernameChange, "Username (tanpa @)", placeholder = "contoh: MrBeast", icon = Icons.Default.Search,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { start() })
                    )
                    RexTonalButton("Tempel username", Icons.Default.ContentPaste, {
                        clipboard.getText()?.text?.trim()?.removePrefix("@")?.takeIf { it.isNotEmpty() }?.let(vm::onUsernameChange)
                    }, Modifier.fillMaxWidth())
                    ModeSwitch(full = fullMode, enabled = !busy, onChange = { fullMode = it }, modifier = Modifier.animateContentSize())
                    RexPrimaryButton(
                        text = if (fullMode) "Stalk Full" else "Stalk",
                        icon = if (fullMode) Icons.Default.Public else Icons.Default.Search,
                        loading = busy, loadingText = if (scraping) "Mengambil data…" else "Mencari…",
                        onClick = { start() }
                    )
                    RexErrorSlot(error, "Terjadi kesalahan")
                }
                Column(Modifier.weight(0.62f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RexOverline("HASIL PROFIL")
                    RexPhaseSwitch(
                        phase = phase,
                        idle = { RexEmptyState(Icons.Default.Search, "Belum ada hasil", "Isi username lalu tekan Stalk.") },
                        loading = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { StatusPanel(scraping, if (scraping) "Membuka halaman & mengambil data…" else state.loadingText); RexSkeletonPanel(media = false) } },
                        result = { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { state.result?.let { BasicProfileCard(it, onOpen = ::openUrl) }; state.fullResult?.let { FullProfileCard(it, onOpen = ::openUrl) } } }
                    )
                }
            }
        }
        scrapeUrl?.let { url ->
            HiddenScraper(url, onResult = { json -> vm.onFullResultReceived(json); scrapeUrl = null }, onError = { msg -> scrapeError = msg; scrapeUrl = null })
        }
    }
}
