// TiktokStalkerScreen.kt
package com.rexaps.rextools.tiktok

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rextools.TiktokStalkResult
import com.rexaps.rextools.ui.*
import kotlinx.coroutines.delay

private const val SCRAPE_JS = """
(function() {
    try {
        var el = document.getElementById('__UNIVERSAL_DATA_FOR_REHYDRATION__');
        if (el) { window.RexBridge.onResult(el.textContent); return; }
        var scripts = document.getElementsByTagName('script');
        for (var i = 0; i < scripts.length; i++) {
            if (scripts[i].id === '__UNIVERSAL_DATA_FOR_REHYDRATION__') {
                window.RexBridge.onResult(scripts[i].textContent);
                return;
            }
        }
        window.RexBridge.onError("Data TikTok tidak ditemukan.");
    } catch (e) {
        window.RexBridge.onError(e.message);
    }
})();
"""

private const val SCRAPE_TIMEOUT_MS = 25_000L

@Composable
fun TiktokStalkerScreen(
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

/* ═════════════════════ WEBVIEW TERSEMBUNYI ═════════════════════ */

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HiddenScraper(url: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
    val resultCb by rememberUpdatedState(onResult)
    val errorCb by rememberUpdatedState(onError)

    Box(Modifier.size(1.dp).alpha(0.01f)) {
        AndroidView(
            factory = { c ->
                val main = Handler(Looper.getMainLooper())
                WebView(c).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString =
                        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    webViewClient = object : WebViewClient() {
                        private var injected = false
                        override fun onPageFinished(view: WebView?, u: String?) {
                            super.onPageFinished(view, u)
                            if (injected) return
                            injected = true
                            view?.postDelayed({ view.evaluateJavascript(SCRAPE_JS, null) }, 4000)
                        }
                    }
                    // Callback jembatan JS datang dari thread lain -> pindahkan ke main thread
                    addJavascriptInterface(object : Any() {
                        @JavascriptInterface
                        fun onResult(json: String) { main.post { resultCb(json) } }

                        @JavascriptInterface
                        fun onError(msg: String) { main.post { errorCb(msg) } }
                    }, "RexBridge")
                    loadUrl(url)
                }
            },
            onRelease = {
                it.stopLoading()
                it.removeJavascriptInterface("RexBridge")
                it.destroy()
            }
        )
    }
}

/* ═════════════════════ MODE SWITCH ═════════════════════ */

@Composable
private fun ModeSwitch(full: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val s = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RexMd)
            .background(s.surfaceContainerLow.copy(alpha = 0.85f))
            .border(hairline(), RexMd)
            .padding(4.dp)
    ) {
        val segW = (maxWidth - 8.dp) / 2
        val x by animateDpAsState(if (full) segW else 0.dp, spring(0.75f, Spring.StiffnessMediumLow), label = "seg")
        Box(
            Modifier
                .offset(x = x)
                .width(segW)
                .fillMaxHeight()
                .clip(RexSm)
                .background(Brush.linearGradient(listOf(s.primary.copy(alpha = 0.30f), s.tertiary.copy(alpha = 0.18f))))
                .border(1.dp, s.primary.copy(alpha = 0.5f), RexSm)
        )
        Row(Modifier.fillMaxSize()) {
            ModeOption("Biasa", "Profil & statistik", Icons.Default.Bolt, !full, enabled, { onChange(false) }, Modifier.weight(1f))
            ModeOption("Full", "Story & video", Icons.Default.Public, full, enabled, { onChange(true) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ModeOption(
    title: String, hint: String, icon: ImageVector,
    selected: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier
) {
    val s = MaterialTheme.colorScheme
    val fg by animateColorAsState(if (selected) s.onSurface else s.onSurfaceVariant, tween(250), label = "fg")
    val tint by animateColorAsState(if (selected) s.primary else s.onSurfaceVariant, tween(250), label = "tint")
    val scale by animateFloatAsState(if (selected) 1f else 0.94f, spring(0.6f), label = "sc")
    Row(
        modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, enabled = enabled, onClick = onClick
            )
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.5f },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = fg)
            Text(hint, style = MaterialTheme.typography.labelSmall, color = s.onSurfaceVariant, maxLines = 1)
        }
    }
}

/* ═════════════════════ STATUS LOADING ═════════════════════ */

@Composable
private fun StatusPanel(scraping: Boolean, text: String) {
    val s = MaterialTheme.colorScheme
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(0.94f, tween(if (scraping) 9000 else 4000, easing = RexEase)) }
    val angle by rememberInfiniteTransition(label = "orbit")
        .animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "a")
    val pulse by rememberInfiniteTransition(label = "pulse")
        .animateFloat(0.25f, 0.8f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")

    RexPanel(glow = true) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier
                    .size(52.dp)
                    .drawBehind {
                        val w = 4.dp.toPx()
                        drawCircle(
                            Brush.radialGradient(listOf(s.primary.copy(alpha = pulse * 0.5f), Color.Transparent)),
                            radius = size.minDimension
                        )
                        inset(w / 2) {
                            drawCircle(s.outlineVariant, style = Stroke(w))
                            drawArc(
                                brush = Brush.sweepGradient(listOf(s.primary, s.tertiary)),
                                startAngle = angle, sweepAngle = 110f, useCenter = false,
                                style = Stroke(w, cap = StrokeCap.Round)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = s.primary, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RexOverline(if (scraping) "Mode Full" else "Mode Biasa")
                AnimatedContent(
                    targetState = text,
                    transitionSpec = {
                        (fadeIn(tween(260, delayMillis = 80)) + slideInVertically(tween(320, easing = RexEase)) { it / 2 }) togetherWith
                            (fadeOut(tween(120)) + slideOutVertically { -it / 2 })
                    },
                    label = "status"
                ) { t -> Text(t, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }
            }
        }
        Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(s.surfaceContainerHighest)) {
            Box(
                Modifier
                    .fillMaxWidth(progress.value)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(s.primary, s.tertiary)))
            )
        }
    }
}

/* ═════════════════════ HEADER PROFIL (dipakai bersama) ═════════════════════ */

@Composable
private fun ProfileHeader(
    avatar: Any?, name: String?, username: String?, bio: String?,
    verified: Boolean, region: String?
) {
    val s = MaterialTheme.colorScheme
    val badge by rememberInfiniteTransition(label = "badge")
        .animateFloat(1f, 1.18f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")
    val drift by rememberInfiniteTransition(label = "banner")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "d")

    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth().height(104.dp).background(
                Brush.linearGradient(
                    listOf(
                        s.primary.copy(alpha = 0.30f + 0.12f * drift),
                        s.tertiary.copy(alpha = 0.28f),
                        s.secondary.copy(alpha = 0.18f + 0.10f * (1 - drift))
                    )
                )
            )
        )
        Column(
            Modifier.fillMaxWidth().padding(top = 52.dp).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RexAvatar(
                model = avatar,
                contentDescription = name?.let { "Foto profil $it" } ?: "Foto profil",
                size = 108.dp,
                modifier = Modifier.entrance(0)
            )
            Column(
                Modifier.entrance(1),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        name ?: "-",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f, fill = false).semantics { heading() }
                    )
                    if (verified) {
                        Icon(
                            Icons.Default.CheckCircle, contentDescription = "Verified", tint = s.primary,
                            modifier = Modifier.size(20.dp).graphicsLayer { scaleX = badge; scaleY = badge }
                        )
                    }
                }
                Text("@${username ?: "-"}", style = MaterialTheme.typography.bodyMedium, color = s.onSurfaceVariant)
            }
            bio?.takeIf { it.isNotBlank() && it != "-" }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.entrance(2))
            }
            region?.takeIf { it.isNotBlank() && it != "-" }?.let {
                RexPill("Region: $it", Modifier.entrance(3), icon = Icons.Default.Public)
            }
        }
    }
}

/* ═════════════════════ KARTU BASIC ═════════════════════ */

@Composable
private fun BasicProfileCard(r: TiktokStalkResult, onOpen: (String) -> Unit) {
    RexPanel(padding = 0.dp, spacing = 0.dp, glow = true) {
        ProfileHeader(
            avatar = r.avatar, name = r.name, username = r.username, bio = r.bio,
            verified = r.verified?.equals("Verified", ignoreCase = true) == true,
            region = r.region
        )
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            r.stats?.let { st ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RexStat("Followers", st.followers ?: "-", Modifier.weight(1f), Icons.Default.Group, 0)
                    RexStat("Following", st.following ?: "-", Modifier.weight(1f), Icons.Default.PersonAdd, 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RexStat("Likes", st.likes ?: "-", Modifier.weight(1f), Icons.Default.FavoriteBorder, 2)
                    RexStat("Videos", st.videos ?: "-", Modifier.weight(1f), Icons.Default.Videocam, 3)
                }
            }
            r.link?.takeIf { it.isNotBlank() }?.let { link ->
                RexTonalButton("Buka di TikTok", Icons.Default.OpenInNew, { onOpen(link) }, Modifier.fillMaxWidth())
            }
        }
    }
}

/* ═════════════════════ KARTU FULL ═════════════════════ */

private const val PREVIEW_COUNT = 6

@Composable
private fun FullProfileCard(r: com.rexaps.rextools.TiktokFullResult, onOpen: (String) -> Unit) {
    val stories = remember(r) { r.stories.entries.toList() }
    val videos = remember(r) { r.videos.entries.toList() }
    var tab by rememberSaveable { mutableIntStateOf(if (stories.isEmpty() && videos.isNotEmpty()) 1 else 0) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    RexPanel(padding = 0.dp, spacing = 0.dp, glow = true) {
        ProfileHeader(
            avatar = r.avatar, name = r.name, username = r.username, bio = r.bio,
            verified = false, region = null
        )
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (stories.isEmpty() && videos.isEmpty()) {
                RexEmptyState(Icons.Default.Videocam, "Tidak ada konten", "Story atau video publik tidak ditemukan.")
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RexFilterChip("Story (${stories.size})", tab == 0) { tab = 0; expanded = false }
                    RexFilterChip("Video (${videos.size})", tab == 1) { tab = 1; expanded = false }
                }

                val items = if (tab == 0) stories else videos
                val shown = if (expanded) items else items.take(PREVIEW_COUNT)

                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        (fadeIn(tween(300, delayMillis = 80)) + slideInVertically(tween(420, easing = RexEase)) { it / 12 }) togetherWith
                            fadeOut(tween(140))
                    },
                    label = "tab"
                ) { t ->
                    Column(Modifier.animateContentSize(spring(0.85f)), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (t == 0) {
                            shown.forEachIndexed { i, e ->
                                StoryRow(e.key.toString(), i, { onOpen(e.value.toString()) })
                            }
                        } else {
                            shown.chunked(2).forEachIndexed { row, pair ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    pair.forEachIndexed { c, e ->
                                        VideoTile(
                                            label = e.key.toString(),
                                            index = row * 2 + c,
                                            onClick = { onOpen(e.value.toString()) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                if (items.size > PREVIEW_COUNT) {
                    RexOutlinedButton(
                        text = if (expanded) "Ciutkan" else "Tampilkan semua (${items.size})",
                        icon = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        onClick = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            r.username?.takeIf { it.isNotBlank() }?.let { u ->
                RexTonalButton(
                    "Buka profil di TikTok", Icons.Default.OpenInNew,
                    { onOpen("https://www.tiktok.com/@$u") }, Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StoryRow(label: String, index: Int, onClick: () -> Unit) {
    val s = MaterialTheme.colorScheme
    RexPanel(
        modifier = Modifier.entrance(index.coerceAtMost(8), 50, 14.dp),
        onClick = onClick, shape = RexMd, padding = 12.dp, spacing = 0.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RexIconTile(Icons.Default.Image, size = 42.dp)
            Text(
                label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.OpenInNew, contentDescription = "Buka", tint = s.primary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun VideoTile(label: String, index: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .height(116.dp)
            .entrance(index.coerceAtMost(9), 45, 16.dp)
            .pressScale(source, 0.94f)
            .clip(RexMd)
            .background(
                Brush.linearGradient(
                    listOf(s.primary.copy(alpha = 0.22f), s.surfaceContainerHigh, s.tertiary.copy(alpha = 0.16f))
                )
            )
            .border(hairline(), RexMd)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = s.onSurface, modifier = Modifier.size(26.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = s.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}