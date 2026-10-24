package com.rexaps.rextools.tiktok

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rextools.ui.*
import kotlinx.coroutines.delay

@Composable
internal fun __splitImpl_TiktokStalkerScreenPortrait(
    onBack: () -> Unit = {},
    vm: TiktokStalkerViewModel = viewModel()
) {
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

    val phase = when {
        busy -> RexPhase.Loading
        hasResult -> RexPhase.Result
        error != null -> RexPhase.Hidden
        else -> RexPhase.Idle
    }

    // Batas waktu supaya WebView tidak menggantung selamanya
    LaunchedEffect(scrapeUrl) {
        if (scrapeUrl != null) {
            delay(SCRAPE_TIMEOUT_MS)
            scrapeError = "Waktu habis. Coba lagi beberapa saat."
            scrapeUrl = null
        }
    }

    // Toast saat profil lengkap berhasil dimuat
    var seenFull by remember { mutableStateOf(state.fullResult != null) }
    LaunchedEffect(state.fullResult) {
        if (state.fullResult != null && !seenFull) toast.show("Profil lengkap dimuat")
        seenFull = state.fullResult != null
    }

    fun openUrl(url: String) {
        if (!url.startsWith("http")) {
            toast.show("Link tidak tersedia", true)
            return
        }
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { toast.show("Tidak bisa membuka link", true) }
    }

    fun start() {
        focus.clearFocus()
        if (busy) return
        scrapeError = null
        val u = state.username.trim().removePrefix("@")
        if (fullMode) {
            if (u.isBlank()) scrapeError = "Isi username dulu."
            else scrapeUrl = "https://www.tiktok.com/@$u"
        } else {
            vm.stalkBasic()
        }
    }

    Box(Modifier.fillMaxSize()) {
        RexScreen(title = "TikTok Stalker", onBack = onBack, toast = toast) {
            RexHero(
                icon = Icons.Default.Person,
                title = "Cari profil TikTok",
                description = "Info profil publik, story, dan video.",
                modifier = Modifier.entrance(0)
            )

            Column(Modifier.entrance(1), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RexField(
                    value = state.username,
                    onChange = vm::onUsernameChange,
                    label = "Username (tanpa @)",
                    placeholder = "contoh: MrBeast",
                    icon = Icons.Default.Search,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { start() })
                )
                RexQuickAction("Tempel", Icons.Default.ContentPaste, {
                    clipboard.getText()?.text?.trim()?.removePrefix("@")
                        ?.takeIf { it.isNotEmpty() }?.let(vm::onUsernameChange)
                })
            }

            ModeSwitch(
                full = fullMode,
                enabled = !busy,
                onChange = { fullMode = it },
                modifier = Modifier.entrance(2)
            )

            RexPrimaryButton(
                text = if (fullMode) "Stalk Full" else "Stalk",
                icon = if (fullMode) Icons.Default.Public else Icons.Default.Search,
                loading = busy,
                loadingText = if (scraping) "Mengambil data…" else "Mencari…",
                modifier = Modifier.entrance(3),
                onClick = { start() }
            )

            RexErrorSlot(error, "Terjadi kesalahan")

            RexPhaseSwitch(
                phase = phase,
                idle = {
                    RexEmptyState(
                        Icons.Default.Search, "Belum ada hasil",
                        "Pilih mode, ketik username, lalu tekan Stalk."
                    )
                },
                loading = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatusPanel(
                            scraping = scraping,
                            text = if (scraping) "Membuka halaman & mengambil data…" else state.loadingText
                        )
                        RexSkeletonPanel(media = false)
                    }
                },
                result = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        state.result?.let { r ->
                            BasicProfileCard(r, onOpen = ::openUrl)
                        }
                        state.fullResult?.let { r ->
                            FullProfileCard(r, onOpen = ::openUrl)
                        }
                    }
                }
            )

            Spacer(Modifier.height(24.dp))
        }

        scrapeUrl?.let { url ->
            HiddenScraper(
                url = url,
                onResult = { json ->
                    vm.onFullResultReceived(json)
                    scrapeUrl = null
                },
                onError = { msg ->
                    scrapeError = msg
                    scrapeUrl = null
                }
            )
        }
    }
}
