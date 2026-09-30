package com.rexaps.rexmusic

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.rexaps.rexmusic.player.EqualizerBars
import com.rexaps.rexmusic.player.MusicPlayerCard
import com.rexaps.rexmusic.player.playerBackground
import java.util.Calendar
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

private const val NOTIF_PERMISSION = "android.permission.POST_NOTIFICATIONS"

// ───────────────────────── Screen (stateful entry point) ─────────────────────────

/** Layar utama RexMusic. Semua state UI lokal ada di sini, child cuma stateless. */
@Composable
fun RexMusicScreen(
    onBack: () -> Unit = {},
    vm: RexMusicViewModel = viewModel()
) {
    val context = LocalContext.current

    // State tanpa posisi -> tidak ikut recompose tiap 500ms. Posisi dibaca terpisah di leaf.
    val state by remember(vm) {
        vm.state.map { it.copy(positionMs = 0L) }.distinctUntilChanged()
    }.collectAsState(initial = vm.state.value.copy(positionMs = 0L))
    val positionState = remember(vm) {
        vm.state.map { it.positionMs }.distinctUntilChanged()
    }.collectAsState(initial = vm.state.value.positionMs)

    var showPlayer by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showRepeat by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val liked = remember { mutableStateMapOf<String, Boolean>() }
    val snackbar = remember { SnackbarHostState() }

    // Pesan singkat dari controller (mis. "Ditambahkan ke antrian").
    LaunchedEffect(vm) {
        vm.state.map { it.notice }.distinctUntilChanged().filterNotNull().collectLatest { message ->
            vm.consumeNotice()
            snackbar.showSnackbar(message)
        }
    }

    // Izin notifikasi (Android 13+), diminta saat pertama kali memutar lagu.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val requestNotifications: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, NOTIF_PERMISSION) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(NOTIF_PERMISSION)
        }
    }

    BackHandler(enabled = showPlayer) { showPlayer = false }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(
                snackbar,
                modifier = Modifier.padding(
                    bottom = if (state.nowPlaying != null && !showPlayer) 84.dp else 0.dp
                )
            )
        }
    ) { pad ->
        AnimatedContent(
            targetState = showPlayer,
            modifier = Modifier.fillMaxSize().padding(pad),
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
            label = "screenSwitch"
        ) { playerVisible ->
            val track = if (playerVisible) state.nowPlaying else null
            if (playerVisible) {
                if (track != null) {
                    PlayerPage(
                        track = track, state = state, positionState = positionState,
                        isLiked = liked[track.id] == true,
                        onPlayPause = { vm.togglePlay() },
                        onNext = { vm.next() },
                        onPrev = { vm.prev() },
                        onOpenPrevious = { vm.prev(force = true) },
                        onSeek = { vm.seekTo(it) },
                        onToggleLike = { liked[track.id] = !(liked[track.id] ?: false) },
                        onRepeat = { showRepeat = true },
                        onAutoplay = { vm.toggleAutoplay() },
                        onQueue = { showQueue = true },
                        onClose = { showPlayer = false }
                    )
                } else {
                    PlayerPlaceholder(state.loadingText) { showPlayer = false }
                }
            } else {
                BrowsePage(
                    state = state, positionState = positionState, query = query,
                    onQueryChange = {
                        query = it
                        if (it.isBlank()) vm.clearSearch() else vm.search(it)
                    },
                    onClear = { query = ""; vm.clearSearch() },
                    onPlaySearch = { requestNotifications(); vm.playFromSearch(it); showPlayer = true },
                    onPlayHistory = { requestNotifications(); vm.playFromHistory(it); showPlayer = true },
                    onPlayMain = { requestNotifications(); vm.playFromMain(it); showPlayer = true },
                    onAddQueue = { vm.addToQueue(it) },
                    onPlayMix = { requestNotifications(); vm.playMix(); showPlayer = true },
                    onOpenQueue = { showQueue = true },
                    onDismissError = { vm.dismissError() },
                    onTogglePlay = { vm.togglePlay() },
                    onNext = { vm.next() },
                    onOpenPlayer = { showPlayer = true },
                    onBack = onBack
                )
            }
        }
    }

    if (showQueue) {
        QueueSheet(
            state = state,
            onDismiss = { showQueue = false },
            onPlay = { vm.playFromQueue(it); showQueue = false },
            onRemove = { vm.removeFromQueue(it) },
            onMoveTop = { vm.moveQueueToTop(it) },
            onClear = { vm.clearQueue() },
            onAutoplay = { vm.toggleAutoplay() }
        )
    }
    if (showRepeat) {
        RepeatSheet(
            total = state.repeatTotal,
            left = state.repeatLeft,
            title = state.nowPlaying?.title.orEmpty(),
            onSet = { vm.setRepeat(it) },
            onDismiss = { showRepeat = false }
        )
    }
}

// ───────────────────────── Player page ─────────────────────────

/** Halaman pemutar: background warna cover, kartu player, error, dan lagu sebelumnya/berikutnya. */
@Composable
private fun PlayerPage(
    track: RexTrack,
    state: RexMusicUiState,
    positionState: State<Long>,
    isLiked: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onOpenPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleLike: () -> Unit,
    onRepeat: () -> Unit,
    onAutoplay: () -> Unit,
    onQueue: () -> Unit,
    onClose: () -> Unit
) {
    var accent by remember(track.cover) { mutableStateOf<Color?>(null) }
    Box(Modifier.fillMaxSize().playerBackground(accent)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            PlayerTopBar(state.userQueue.size, onQueue, onClose)
            MusicPlayerCard(
                track = track, isPlaying = state.isPlaying, positionState = positionState,
                durationMs = state.durationMs, isLiked = isLiked,
                accent = accent, onAccentFound = { accent = it },
                onPlayPause = onPlayPause, onNext = onNext, onPrev = onPrev, onSeek = onSeek,
                onToggleLike = onToggleLike,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                phase = state.phase, bufferedPercent = state.bufferedPercent,
                repeatTotal = state.repeatTotal, repeatLeft = state.repeatLeft,
                autoplay = state.autoplay, queueCount = state.userQueue.size,
                onRepeat = onRepeat, onAutoplay = onAutoplay, onQueue = onQueue,
                onSwipePrev = onOpenPrevious
            )
            PlayerError(state.error, onRetry = onPlayPause)
            NeighborSection(state, onOpenPrevious, onNext)
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Top bar player: tutup (panah bawah), judul, dan tombol antrian dengan badge. */
@Composable
private fun PlayerTopBar(queueCount: Int, onQueue: () -> Unit, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Tutup player",
                modifier = Modifier.size(32.dp))
        }
        Text("RexMusic", modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        IconButton(onClick = onQueue) {
            BadgedBox(badge = { if (queueCount > 0) Badge { Text("$queueCount") } }) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Antrian")
            }
        }
    }
}

/** Tampil sementara saat Mix sedang memilih lagu dan belum ada lagu yang aktif. */
@Composable
private fun PlayerPlaceholder(loadingText: String, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PlayerTopBar(0, {}, onClose)
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(16.dp))
            Text(loadingText.ifBlank { "Memuat..." }, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Banner error di halaman player dengan tombol "Coba lagi" (putar ulang lewat API). */
@Composable
private fun PlayerError(error: String?, onRetry: () -> Unit) {
    AnimatedVisibility(visible = !error.isNullOrBlank()) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer
        ) {
            Row(
                Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.width(8.dp))
                Text(error.orEmpty(), modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Coba lagi")
                }
            }
        }
    }
}

/** Lagu sebelumnya (riwayat) dan berikutnya (antrian / daftar / autoplay pintar). */
@Composable
private fun NeighborSection(state: RexMusicUiState, onPrevious: () -> Unit, onNext: () -> Unit) {
    val previous = state.previous
    val upNext = state.upNext
    if (previous == null && upNext == null && !state.autoplay) return
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (previous != null) {
            NeighborRow("SEBELUMNYA", previous, Icons.Default.SkipPrevious, onPrevious)
        }
        if (upNext != null) {
            val label = if (state.userQueue.isNotEmpty()) "DI ANTRIAN" else "SELANJUTNYA"
            NeighborRow(label, upNext, Icons.Default.SkipNext, onNext)
        } else if (state.autoplay) {
            AutoplayRow(state.favoriteArtists, state.popularCount, onNext)
        }
    }
}

@Composable
private fun NeighborRow(label: String, track: RexTrack, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CoverThumb(track.cover, 48.dp)
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                Text(track.title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Kartu pengganti "selanjutnya" saat lagu berikutnya dipilih otomatis oleh autoplay pintar. */
@Composable
private fun AutoplayRow(favorites: List<String>, popularCount: Int, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val subtitle = when {
        favorites.isNotEmpty() -> "Dicampur dari ${favorites.joinToString(", ")} & lagu populer"
        popularCount > 0 -> "Dipilih acak dari lagu populer"
        else -> "Dipilih otomatis untukmu"
    }
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = scheme.primary.copy(alpha = 0.10f)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(scheme.primary, scheme.tertiary))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = scheme.onPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text("AUTOPLAY PINTAR", style = MaterialTheme.typography.labelSmall, color = scheme.primary)
                Text("Lagu berikutnya untukmu", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.SkipNext, contentDescription = "Lagu berikutnya",
                tint = scheme.onSurfaceVariant)
        }
    }
}

// ───────────────────────── Browse page ─────────────────────────

/** Halaman browse: header, search, status, list/empty/skeleton, dan mini player di bawah. */
@Composable
private fun BrowsePage(
    state: RexMusicUiState,
    positionState: State<Long>,
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onPlaySearch: (Int) -> Unit,
    onPlayHistory: (Int) -> Unit,
    onPlayMain: (Int) -> Unit,
    onAddQueue: (RexTrack) -> Unit,
    onPlayMix: () -> Unit,
    onOpenQueue: () -> Unit,
    onDismissError: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onOpenPlayer: () -> Unit,
    onBack: () -> Unit
) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val noData = state.searchResults.isEmpty() && state.tracks.isEmpty() && state.history.isEmpty()
    val nowPlaying = state.nowPlaying

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(greetingForHour(hour), "RexMusic", onBack)
            RexSearchBar(query, onQueryChange, onClear, Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            LoadingHint(state.loading, state.loadingText)
            ErrorBanner(state.error, onDismissError)
            when {
                state.loading && noData -> SkeletonList()
                noData && state.error == null -> EmptyState(hasQuery = query.isNotBlank())
                else -> TrackList(
                    state = state,
                    showMix = query.isBlank(),
                    bottomPadding = if (nowPlaying != null) 112.dp else 32.dp,
                    onPlaySearch = onPlaySearch,
                    onPlayHistory = onPlayHistory,
                    onPlayMain = onPlayMain,
                    onAddQueue = onAddQueue,
                    onPlayMix = onPlayMix,
                    onOpenQueue = onOpenQueue
                )
            }
        }

        AnimatedVisibility(
            visible = nowPlaying != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            if (nowPlaying != null) {
                MiniPlayer(
                    track = nowPlaying, isPlaying = state.isPlaying,
                    busy = state.phase.isPlayerBusy, durationMs = state.durationMs,
                    positionState = positionState, onToggle = onTogglePlay,
                    onNext = onNext, onOpen = onOpenPlayer
                )
            }
        }
    }
}

/** Kartu besar "Mix Untukmu": satu tap memutar lagu pilihan pintar. */
@Composable
private fun MixHeroCard(favorites: List<String>, popularCount: Int, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val subtitle = when {
        favorites.isNotEmpty() -> "Berdasarkan ${favorites.joinToString(", ")}"
        popularCount > 0 -> "Acak dari $popularCount lagu populer"
        else -> "Putar beberapa lagu, Mix akan belajar seleramu"
    }
    val source = remember { MutableInteractionSource() }
    val scale = rememberPressScale(source)
    Surface(
        onClick = onClick,
        interactionSource = source,
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent
    ) {
        Row(
            Modifier
                .background(Brush.linearGradient(listOf(scheme.primary, scheme.tertiary)))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null,
                        tint = scheme.onPrimary, modifier = Modifier.size(16.dp))
                    Text("MIX UNTUKMU", style = MaterialTheme.typography.labelSmall,
                        color = scheme.onPrimary.copy(alpha = 0.85f), letterSpacing = 1.5.sp)
                }
                Spacer(Modifier.height(6.dp))
                Text("Putar Mix Pintar", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = scheme.onPrimary)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = scheme.onPrimary.copy(alpha = 0.85f),
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(16.dp))
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(scheme.onPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Putar Mix",
                    tint = scheme.onPrimary, modifier = Modifier.size(30.dp))
            }
        }
    }
}

/** Ringkasan antrian di halaman browse; tap untuk membuka daftar lengkap. */
@Composable
private fun QueueSummaryCard(queue: List<RexTrack>, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = scheme.secondaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(scheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = scheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text("Antrian · ${queue.size} lagu", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Text("Berikutnya: ${queue.first().title}", style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Buka antrian",
                tint = scheme.onSurfaceVariant)
        }
    }
}

// ───────────────────────── Mini player ─────────────────────────

/** Mini player melayang di bawah halaman browse: cover, judul, play/pause, next, progres tipis. */
@Composable
private fun MiniPlayer(
    track: RexTrack,
    isPlaying: Boolean,
    busy: Boolean,
    durationMs: Long,
    positionState: State<Long>,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        color = scheme.surfaceVariant,
        tonalElevation = 3.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, scheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column {
            Row(
                Modifier.padding(start = 10.dp, end = 6.dp, top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CoverThumb(track.cover, 48.dp)
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(42.dp)
                        .background(Brush.linearGradient(listOf(scheme.primary, scheme.tertiary)), CircleShape)
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            color = scheme.onPrimary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Jeda" else "Putar",
                            tint = scheme.onPrimary
                        )
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Lagu berikutnya")
                }
            }
            MiniProgress(positionState, durationMs)
        }
    }
}

/** Garis progres tipis. Membaca posisi sendiri supaya hanya bagian ini yang recompose. */
@Composable
private fun MiniProgress(positionState: State<Long>, durationMs: Long) {
    val scheme = MaterialTheme.colorScheme
    val progress = if (durationMs > 0) {
        (positionState.value.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    Box(Modifier.fillMaxWidth().height(3.dp).background(scheme.onSurface.copy(alpha = 0.1f))) {
        Box(
            Modifier.fillMaxWidth(progress).fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(scheme.primary, scheme.tertiary)))
        )
    }
}

// ───────────────────────── Header & Search ─────────────────────────

/** Header custom dengan gradient tipis dari warna primary + sapaan. */
@Composable
private fun ScreenHeader(greeting: String, title: String, onBack: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val brush = remember(primary) {
        Brush.verticalGradient(listOf(primary.copy(alpha = 0.18f), Color.Transparent))
    }
    Row(
        modifier = Modifier.fillMaxWidth().background(brush)
            .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Kembali")
        }
        Column {
            Text(greeting, style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

/** Search bar pill dengan shadow halus dan tombol clear beranimasi. */
@Composable
private fun RexSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shadowElevation = 4.dp
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Cari lagu di Spotify") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
            trailingIcon = { ClearButton(visible = query.isNotEmpty(), onClear = onClear) },
            singleLine = true,
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Tombol clear yang muncul/hilang dengan fade + scale. */
@Composable
private fun ClearButton(visible: Boolean, onClear: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        IconButton(onClick = onClear) {
            Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian")
        }
    }
}

// ───────────────────────── Status: loading / error / empty ─────────────────────────

/** Progress bar tipis + teks status saat loading. */
@Composable
private fun LoadingHint(loading: Boolean, loadingText: String) {
    AnimatedVisibility(visible = loading) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            LinearProgressIndicator(Modifier.fillMaxWidth().clip(CircleShape))
            Text(
                text = loadingText.ifBlank { "Memuat..." },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/** Banner error ringkas dengan tombol tutup, muncul hanya kalau ada pesan. */
@Composable
private fun ErrorBanner(error: String?, onDismiss: () -> Unit) {
    AnimatedVisibility(visible = !error.isNullOrBlank()) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer
        ) {
            Row(
                Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.width(8.dp))
                Text("Terjadi masalah: ${error.orEmpty()}",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup pesan",
                        tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}

/** Empty state: ikon + judul + hint. Beda teks kalau user sedang mencari. */
@Composable
private fun EmptyState(hasQuery: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(88.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(if (hasQuery) "Lagu tidak ditemukan" else "Belum ada lagu",
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("Coba kata kunci lain, misalnya judul lagu atau nama artis.",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ───────────────────────── Skeleton / shimmer ─────────────────────────

/** Brush shimmer yang bergerak; hanya hidup selama skeleton tampil. */
@Composable
private fun rememberShimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val x by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f, targetValue = 900f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmerX"
    )
    return Brush.linearGradient(
        colors = listOf(base.copy(alpha = 0.35f), base.copy(alpha = 0.85f), base.copy(alpha = 0.35f)),
        start = Offset(x - 300f, 0f),
        end = Offset(x, 0f)
    )
}

/** Daftar placeholder saat data pertama kali dimuat. */
@Composable
private fun SkeletonList() {
    val brush = rememberShimmerBrush()
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(6) { SkeletonRow(brush) }
    }
}

/** Satu baris skeleton: kotak cover + dua garis teks. */
@Composable
private fun SkeletonRow(brush: Brush) {
    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(brush))
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(6.dp)).background(brush))
            Box(Modifier.fillMaxWidth(0.4f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(brush))
        }
    }
}

// ───────────────────────── List & sections ─────────────────────────

/** Satu LazyColumn: Mix, ringkasan antrian, hasil pencarian, riwayat (horizontal), rekomendasi. */
@Composable
private fun TrackList(
    state: RexMusicUiState,
    showMix: Boolean,
    bottomPadding: Dp,
    onPlaySearch: (Int) -> Unit,
    onPlayHistory: (Int) -> Unit,
    onPlayMain: (Int) -> Unit,
    onAddQueue: (RexTrack) -> Unit,
    onPlayMix: () -> Unit,
    onOpenQueue: () -> Unit
) {
    val queuedIds = remember(state.userQueue) { state.userQueue.map { it.id }.toSet() }
    val nowId = state.nowPlaying?.id
    val loadingId = if (state.phase.isPlayerBusy) nowId else null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showMix) {
            item(key = "mix") {
                MixHeroCard(state.favoriteArtists, state.popularCount, onPlayMix)
            }
        }
        if (state.userQueue.isNotEmpty()) {
            item(key = "queue") { QueueSummaryCard(state.userQueue, onOpenQueue) }
        }

        trackSection(
            "Hasil Pencarian", "s", state.searchResults, nowId, state.isPlaying,
            loadingId, queuedIds, onPlaySearch, onAddQueue
        )

        if (state.history.isNotEmpty()) {
            item(key = "header-h") {
                SectionHeader("Terakhir Diputar", state.history.size, Modifier.padding(top = 14.dp, bottom = 2.dp))
            }
            item(key = "row-h") {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(state.history, key = { _, t -> "h-${t.id}" }) { idx, track ->
                        val active = track.id == nowId
                        HistoryCard(
                            track = track, active = active, playing = active && state.isPlaying,
                            onClick = { onPlayHistory(idx) }
                        )
                    }
                }
            }
        }

        trackSection(
            "Rekomendasi", "m", state.tracks, nowId, state.isPlaying,
            loadingId, queuedIds, onPlayMain, onAddQueue
        )
    }
}

/** Menambah header + item lagu ke LazyColumn. Key tetap "s-id" / "m-id". */
private fun LazyListScope.trackSection(
    title: String,
    keyPrefix: String,
    tracks: List<RexTrack>,
    nowId: String?,
    isPlaying: Boolean,
    loadingId: String?,
    queuedIds: Set<String>,
    onTrackClick: (Int) -> Unit,
    onAddQueue: (RexTrack) -> Unit
) {
    if (tracks.isEmpty()) return
    item(key = "header-$keyPrefix") {
        SectionHeader(title, tracks.size, Modifier.padding(top = 14.dp, bottom = 2.dp))
    }
    itemsIndexed(tracks, key = { _, t -> "$keyPrefix-${t.id}" }) { idx, track ->
        val active = track.id == nowId
        StaggerIn(index = idx, key = "$keyPrefix-${track.id}") {
            TrackCard(
                track = track,
                active = active,
                playing = active && isPlaying,
                loading = track.id == loadingId,
                queued = track.id in queuedIds,
                onClick = { onTrackClick(idx) },
                onQueue = { onAddQueue(track) }
            )
        }
    }
}

/** Header section: judul, badge jumlah, dan aksi opsional ("Lihat semua"). */
@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        CountBadge(count)
        Spacer(Modifier.weight(1f))
        if (actionLabel != null) TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

/** Pill kecil berisi jumlah item. */
@Composable
private fun CountBadge(count: Int) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

// ───────────────────────── History card (horizontal) ─────────────────────────

/** Kartu besar untuk baris "Terakhir Diputar". Tap = putar ulang (audio diambil lagi lewat API). */
@Composable
private fun HistoryCard(track: RexTrack, active: Boolean, playing: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val scale = rememberPressScale(source)
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        interactionSource = source,
        modifier = Modifier.width(140.dp).graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(140.dp).clip(RoundedCornerShape(20.dp)).background(scheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (track.cover.isNotBlank()) {
                    AsyncImage(
                        model = track.cover, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.MusicNote, contentDescription = null,
                        tint = scheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                }
                if (active) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (playing) EqualizerBars(color = Color.White, height = 22.dp, barWidth = 4.dp)
                        else Icon(Icons.Default.PlayArrow, contentDescription = "Putar", tint = Color.White)
                    }
                }
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(8.dp).size(30.dp)
                        .clip(CircleShape).background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null,
                        tint = scheme.onPrimary, modifier = Modifier.size(18.dp))
                }
            }
            Column(Modifier.padding(horizontal = 4.dp)) {
                Text(track.title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) scheme.primary else scheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ───────────────────────── Track card ─────────────────────────

/** Adapter dari RexTrack ke kartu stateless (supaya gampang di-preview). */
@Composable
private fun TrackCard(
    track: RexTrack,
    active: Boolean,
    playing: Boolean,
    loading: Boolean,
    queued: Boolean,
    onClick: () -> Unit,
    onQueue: () -> Unit
) {
    TrackCardContent(
        title = track.title,
        artist = track.artist,
        cover = track.cover,
        onClick = onClick,
        loading = loading,
        active = active,
        playing = playing,
        queued = queued,
        onQueue = onQueue
    )
}

/** Kartu lagu: cover, teks, tombol tambah-antrian, badge play/equalizer/spinner. */
@Composable
private fun TrackCardContent(
    title: String,
    artist: String,
    cover: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    loading: Boolean = false,
    active: Boolean = false,
    playing: Boolean = false,
    queued: Boolean = false,
    onQueue: (() -> Unit)? = null
) {
    val source = remember { MutableInteractionSource() }
    val scale = rememberPressScale(source)
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        interactionSource = source,
        modifier = modifier.fillMaxWidth().graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
        shape = RoundedCornerShape(18.dp),
        color = if (active) primary.copy(alpha = 0.14f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = if (active) BorderStroke(1.dp, primary.copy(alpha = 0.35f)) else null
    ) {
        Row(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(Color.Transparent, primary.copy(alpha = 0.06f))))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CoverThumb(cover)
            TrackTexts(title, artist, badge, active, Modifier.weight(1f))
            if (onQueue != null) QueueAddButton(queued, onQueue)
            PlayBadgeButton(loading, active && playing)
        }
    }
}

/** Tombol kecil tambah ke antrian; berubah jadi centang kalau sudah ada di antrian. */
@Composable
private fun QueueAddButton(queued: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp).clip(CircleShape)
            .background(scheme.onSurface.copy(alpha = if (queued) 0.14f else 0.07f))
    ) {
        Icon(
            if (queued) Icons.Default.Check else Icons.Default.Add,
            contentDescription = if (queued) "Sudah di antrian" else "Tambah ke antrian",
            tint = if (queued) scheme.primary else scheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Thumbnail rounded 12dp (ukuran bisa diatur); fallback ikon kalau cover kosong. */
@Composable
private fun CoverThumb(cover: String, size: Dp = 56.dp) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (cover.isNotBlank()) {
            AsyncImage(
                model = cover, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Default.MusicNote, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Judul (+ badge opsional) dan artist, masing-masing 1 baris ellipsis. */
@Composable
private fun TrackTexts(
    title: String,
    artist: String,
    badge: String?,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            if (badge != null) {
                Spacer(Modifier.width(6.dp))
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(badge, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                }
            }
        }
        Text(artist, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Lingkaran kecil: spinner (loading), equalizer (sedang main), atau ikon play. */
@Composable
private fun PlayBadgeButton(loading: Boolean, playing: Boolean) {
    Box(
        modifier = Modifier.size(36.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        when {
            loading -> CircularProgressIndicator(
                strokeWidth = 2.dp, modifier = Modifier.size(18.dp),
                color = MaterialTheme.colorScheme.primary
            )
            playing -> EqualizerBars(color = MaterialTheme.colorScheme.primary)
            else -> Icon(Icons.Default.PlayArrow, contentDescription = "Putar",
                tint = MaterialTheme.colorScheme.primary)
        }
    }
}

// ───────────────────────── Queue sheet ─────────────────────────

/** Bottom sheet antrian: toggle autoplay, lagu sekarang, daftar antrian (putar / naikkan / hapus). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(
    state: RexMusicUiState,
    onDismiss: () -> Unit,
    onPlay: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMoveTop: (Int) -> Unit,
    onClear: () -> Unit,
    onAutoplay: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surface
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Antrian", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                CountBadge(state.userQueue.size)
                Spacer(Modifier.weight(1f))
                if (state.userQueue.isNotEmpty()) {
                    TextButton(onClick = onClear) { Text("Kosongkan") }
                }
            }
            Spacer(Modifier.height(8.dp))
            AutoplayToggleRow(state.autoplay, state.favoriteArtists, state.popularCount, onAutoplay)

            state.nowPlaying?.let { now ->
                Spacer(Modifier.height(12.dp))
                NeighborRow("SEDANG DIPUTAR", now, Icons.Default.MusicNote) {}
            }

            Spacer(Modifier.height(16.dp))
            Text("BERIKUTNYA DALAM ANTRIAN", style = MaterialTheme.typography.labelSmall,
                color = scheme.primary)
            Spacer(Modifier.height(8.dp))

            if (state.userQueue.isEmpty()) {
                Text(
                    "Antrian kosong. Tambahkan lagu dengan tombol + di daftar lagu." +
                        if (state.autoplay) " Setelah itu, lagu dipilih otomatis oleh Autoplay pintar." else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(state.userQueue, key = { _, t -> t.id }) { idx, track ->
                        QueueRow(
                            index = idx, track = track,
                            onPlay = { onPlay(idx) },
                            onMoveTop = { onMoveTop(idx) },
                            onRemove = { onRemove(idx) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoplayToggleRow(
    autoplay: Boolean,
    favorites: List<String>,
    popularCount: Int,
    onToggle: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val subtitle = when {
        !autoplay -> "Mati: lagu berikutnya mengikuti urutan daftar"
        favorites.isNotEmpty() -> "Dicampur dari ${favorites.joinToString(", ")} & lagu populer"
        popularCount > 0 -> "Lagu berikutnya acak dari lagu populer"
        else -> "Lagu berikutnya dipilih otomatis"
    }
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(18.dp),
        color = scheme.primary.copy(alpha = if (autoplay) 0.12f else 0.05f)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = scheme.primary)
            Column(Modifier.weight(1f)) {
                Text("Autoplay pintar", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Switch(checked = autoplay, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
private fun QueueRow(
    index: Int,
    track: RexTrack,
    onPlay: () -> Unit,
    onMoveTop: () -> Unit,
    onRemove: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onPlay,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = scheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("${index + 1}", style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant, modifier = Modifier.width(18.dp),
                textAlign = TextAlign.Center)
            CoverThumb(track.cover, 44.dp)
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (index > 0) {
                IconButton(onClick = onMoveTop) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Naikkan ke paling atas")
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Hapus dari antrian")
            }
        }
    }
}

// ───────────────────────── Repeat sheet ─────────────────────────

/** Bottom sheet ulangi lagu: stepper, slider 0-50, dan preset cepat. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepeatSheet(
    total: Int,
    left: Int,
    title: String,
    onSet: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surface
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Ulangi lagu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                StepButton(Icons.Default.Remove, "Kurangi", enabled = total > 0) { onSet(total - 1) }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(132.dp)) {
                    Text(
                        if (total == 0) "Mati" else "$total×",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (total == 0) scheme.onSurface else scheme.primary
                    )
                    Text(
                        if (total == 0) "lagu lanjut seperti biasa" else "sisa $left kali lagi",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                StepButton(Icons.Default.Add, "Tambah", enabled = total < 50) { onSet(total + 1) }
            }

            Column {
                Slider(
                    value = total.toFloat(),
                    onValueChange = { onSet(it.roundToInt()) },
                    valueRange = 0f..50f,
                    steps = 49
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    Text("50", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(0, 2, 5, 10, 25, 50).forEach { n ->
                    PresetPill(if (n == 0) "Mati" else "$n×", selected = total == n) { onSet(n) }
                }
            }

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Selesai", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(52.dp)
            .background(scheme.onSurface.copy(alpha = if (enabled) 0.10f else 0.04f), CircleShape)
    ) {
        Icon(icon, contentDescription = description)
    }
}

@Composable
private fun PresetPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        if (selected) scheme.primary else scheme.onSurface.copy(alpha = 0.08f), label = "presetBg"
    )
    val fg by animateColorAsState(
        if (selected) scheme.onPrimary else scheme.onSurface, label = "presetFg"
    )
    Box(
        Modifier.clip(CircleShape).background(bg).clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

// ───────────────────────── Motion helpers ─────────────────────────

/** Scale kecil saat ditekan. Return State supaya dibaca di graphicsLayer (tanpa recompose). */
@Composable
private fun rememberPressScale(source: MutableInteractionSource): State<Float> {
    val pressed by source.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
}

/** Fade + slide-up dengan delay per index (50ms, max 8 item). Hanya jalan sekali per key. */
@Composable
private fun StaggerIn(index: Int, key: String, content: @Composable () -> Unit) {
    var shown by rememberSaveable(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        if (!shown) {
            delay(index.coerceAtMost(8) * 50L)
            shown = true
        }
    }
    val progress by animateFloatAsState(if (shown) 1f else 0f, tween(320), label = "stagger")
    Box(
        Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 24.dp.toPx()
        }
    ) { content() }
}

private fun greetingForHour(hour: Int): String = when (hour) {
    in 4..10 -> "Selamat pagi"
    in 11..14 -> "Selamat siang"
    in 15..17 -> "Selamat sore"
    else -> "Selamat malam"
}

// ───────────────────────── Previews ─────────────────────────

@Preview(showBackground = true)
@Composable
private fun TrackCardPreview() {
    MaterialTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TrackCardContent("Judul Lagu Yang Cukup Panjang Sekali", "Nama Artis", "", {},
                badge = "Popular", onQueue = {})
            TrackCardContent("Sedang Dimuat", "Nama Artis", "", {}, loading = true, onQueue = {})
            TrackCardContent("Sedang Diputar", "Nama Artis", "", {}, active = true, playing = true,
                queued = true, onQueue = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MixHeroPreview() {
    MaterialTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MixHeroCard(listOf("Tulus", "Hindia"), 30) {}
            MixHeroCard(emptyList(), 0) {}
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchAndHeaderPreview() {
    MaterialTheme {
        Column {
            ScreenHeader("Selamat malam", "RexMusic") {}
            RexSearchBar("dewa", {}, {}, Modifier.padding(20.dp))
            SectionHeader("Rekomendasi", 8, Modifier.padding(horizontal = 20.dp), "Lihat semua")
        }
    }
}

@Preview(showBackground = true, heightDp = 480)
@Composable
private fun EmptyStatePreview() {
    MaterialTheme { EmptyState(hasQuery = true) }
}

@Preview(showBackground = true, heightDp = 480)
@Composable
private fun SkeletonPreview() {
    MaterialTheme { SkeletonList() }
}
