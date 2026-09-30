package com.rexaps.rexmusic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.semantics.Role
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

/** Semua aksi yang dipakai halaman browse, dibungkus supaya parameter tidak berantakan. */
private class LibraryActions(
    val onPlaySearch: (Int) -> Unit,
    val onPlayHistory: (Int) -> Unit,
    val onPlayMain: (Int) -> Unit,
    val onPlayOffline: (RexTrack) -> Unit,
    val onPlayOfflineAll: (Boolean) -> Unit,
    val onAddQueue: (RexTrack) -> Unit,
    val onPlayMix: () -> Unit,
    val onOpenQueue: () -> Unit,
    val onDownload: (RexTrack) -> Unit,
    val onCancelDownload: (RexTrack) -> Unit,
    val onDelete: (RexTrack) -> Unit,
    val onDeleteAll: () -> Unit,
    val onRequestAccess: () -> Unit,
    val onSetOffline: (Boolean) -> Unit,
    val onDismissError: () -> Unit,
    val onTogglePlay: () -> Unit,
    val onNext: () -> Unit,
    val onOpenPlayer: () -> Unit
)

private enum class CoverOverlay { None, Loading, Playing, Paused }

private fun overlayFor(active: Boolean, playing: Boolean, loading: Boolean): CoverOverlay = when {
    loading -> CoverOverlay.Loading
    active && playing -> CoverOverlay.Playing
    active -> CoverOverlay.Paused
    else -> CoverOverlay.None
}

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
    var showStorageDialog by remember { mutableStateOf(false) }
    var showDeleteAll by remember { mutableStateOf(false) }
    var pendingDownload by remember { mutableStateOf<RexTrack?>(null) }
    var deleteTarget by remember { mutableStateOf<RexTrack?>(null) }
    var query by remember { mutableStateOf("") }
    val liked = remember { mutableStateMapOf<String, Boolean>() }
    val snackbar = remember { SnackbarHostState() }

    // Muat ulang daftar unduhan saat layar dibuka.
    LaunchedEffect(vm) { vm.refreshOffline() }

    // Pesan singkat dari controller (mis. "Ditambahkan ke antrian").
    LaunchedEffect(vm) {
        vm.state.map { it.notice }.distinctUntilChanged().filterNotNull().collectLatest { message ->
            vm.consumeNotice()
            snackbar.showSnackbar(message)
        }
    }

    // Izin notifikasi (Android 13+), diminta saat pertama kali memutar lagu.
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val requestNotifications: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, NOTIF_PERMISSION) != PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(NOTIF_PERMISSION)
        }
    }

    // Izin penyimpanan untuk Download/RexAps/Music.
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { vm.refreshOffline() }
    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { vm.refreshOffline() }
    val openStorageSettings: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appIntent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            try {
                settingsLauncher.launch(appIntent)
            } catch (_: Exception) {
                settingsLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            storageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    // Izin sudah diberikan -> lanjutkan unduhan yang tertunda.
    LaunchedEffect(state.offline.hasAccess) {
        val pending = pendingDownload
        if (state.offline.hasAccess && pending != null) {
            pendingDownload = null
            showStorageDialog = false
            vm.download(pending)
        }
    }

    val downloadTrack: (RexTrack) -> Unit = { track ->
        if (RexOfflineManager.hasAccess(context)) {
            vm.download(track)
        } else {
            pendingDownload = track
            showStorageDialog = true
        }
    }

    BackHandler(enabled = showPlayer) { showPlayer = false }

    val actions = LibraryActions(
        onPlaySearch = { requestNotifications(); vm.playFromSearch(it); showPlayer = true },
        onPlayHistory = { requestNotifications(); vm.playFromHistory(it); showPlayer = true },
        onPlayMain = { requestNotifications(); vm.playFromMain(it); showPlayer = true },
        onPlayOffline = { requestNotifications(); vm.playFromOffline(it); showPlayer = true },
        onPlayOfflineAll = { shuffle ->
            requestNotifications(); vm.playOfflineAll(shuffle); showPlayer = true
        },
        onAddQueue = { vm.addToQueue(it) },
        onPlayMix = { requestNotifications(); vm.playMix(); showPlayer = true },
        onOpenQueue = { showQueue = true },
        onDownload = downloadTrack,
        onCancelDownload = { vm.cancelDownload(it) },
        onDelete = { deleteTarget = it },
        onDeleteAll = { showDeleteAll = true },
        onRequestAccess = { pendingDownload = null; showStorageDialog = true },
        onSetOffline = { on ->
            if (on != state.offline.enabled) {
                query = ""
                vm.setOfflineMode(on)
            }
        },
        onDismissError = { vm.dismissError() },
        onTogglePlay = { vm.togglePlay() },
        onNext = { vm.next() },
        onOpenPlayer = { showPlayer = true }
    )

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
                        onDownload = { downloadTrack(track) },
                        onCancelDownload = { vm.cancelDownload(track) },
                        onDelete = { deleteTarget = track },
                        onClose = { showPlayer = false }
                    )
                } else {
                    PlayerPlaceholder(state.loadingText) { showPlayer = false }
                }
            } else {
                BrowsePage(
                    state = state, positionState = positionState, query = query,
                    actions = actions,
                    onQueryChange = {
                        query = it
                        if (!state.offline.enabled) {
                            if (it.isBlank()) vm.clearSearch() else vm.search(it)
                        }
                    },
                    onClear = { query = ""; vm.clearSearch() },
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

    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = { showStorageDialog = false; pendingDownload = null },
            icon = { Icon(Icons.Default.Folder, contentDescription = null) },
            title = { Text("Izinkan akses penyimpanan") },
            text = {
                Text(
                    "Lagu yang diunduh disimpan di ${RexOfflineManager.DISPLAY_PATH}/ " +
                        "supaya mudah kamu buka dan hapus lewat aplikasi File. " +
                        "Aplikasi butuh izin akses penyimpanan untuk itu."
                )
            },
            confirmButton = {
                TextButton(onClick = { openStorageSettings() }) { Text("Buka pengaturan") }
            },
            dismissButton = {
                TextButton(onClick = { showStorageDialog = false; pendingDownload = null }) {
                    Text("Nanti")
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Hapus dari offline?") },
            text = {
                Text(
                    "\"${target.title}\" akan dihapus dari ${RexOfflineManager.DISPLAY_PATH}/. " +
                        "Kamu tetap bisa memutarnya lagi saat online."
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.deleteDownload(target); deleteTarget = null }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Batal") } }
        )
    }

    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = { showDeleteAll = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Hapus semua lagu offline?") },
            text = {
                Text(
                    "${state.offline.entries.size} lagu " +
                        "(${formatBytes(state.offline.totalBytes)}) akan dihapus dari perangkat."
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.deleteAllDownloads(); showDeleteAll = false }) {
                    Text("Hapus semua", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteAll = false }) { Text("Batal") } }
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
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    var accent by remember(track.cover) { mutableStateOf<Color?>(null) }
    val dlKey = remember(track.title, track.artist) { offlineKey(track) }
    val downloaded = state.offline.isDownloaded(track, dlKey)
    val status = state.offline.downloads[dlKey]

    Box(Modifier.fillMaxSize().playerBackground(accent)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            PlayerTopBar(
                queueCount = state.userQueue.size,
                offlinePlaying = state.nowPlayingOffline,
                onQueue = onQueue,
                onClose = onClose,
                downloadSlot = {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        DownloadButton(downloaded, status, onDownload, onCancelDownload, onDelete)
                    }
                }
            )
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

/** Top bar player: tutup, judul (+ penanda offline), slot unduh, dan tombol antrian dengan badge. */
@Composable
private fun PlayerTopBar(
    queueCount: Int,
    offlinePlaying: Boolean,
    onQueue: () -> Unit,
    onClose: () -> Unit,
    downloadSlot: @Composable () -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Tutup player",
                modifier = Modifier.size(32.dp))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("RexMusic", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (offlinePlaying) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.OfflinePin, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                    Text("DIPUTAR OFFLINE", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                }
            }
        }
        downloadSlot()
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
        PlayerTopBar(0, false, {}, onClose)
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

/** Banner error di halaman player dengan tombol "Coba lagi". */
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
            AutoplayRow(state.favoriteArtists, state.popularCount, state.offline.enabled, onNext)
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
private fun AutoplayRow(
    favorites: List<String>,
    popularCount: Int,
    offlineMode: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val subtitle = when {
        offlineMode -> "Dipilih dari lagu unduhanmu"
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

/** Halaman browse: header + tombol mode, search, lalu daftar online ATAU perpustakaan offline, + mini player. */
@Composable
private fun BrowsePage(
    state: RexMusicUiState,
    positionState: State<Long>,
    query: String,
    actions: LibraryActions,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val offlineMode = state.offline.enabled
    val noData = state.searchResults.isEmpty() && state.tracks.isEmpty() && state.history.isEmpty()
    val nowPlaying = state.nowPlaying
    val bottomPadding = if (nowPlaying != null) 112.dp else 32.dp

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                greeting = greetingForHour(hour),
                title = "RexMusic",
                offline = offlineMode,
                onSetOffline = actions.onSetOffline,
                onBack = onBack
            )
            RexSearchBar(
                query, onQueryChange, onClear,
                Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                placeholder = if (offlineMode) "Cari di lagu offline" else "Cari lagu di Spotify"
            )
            if (!offlineMode) LoadingHint(state.loading, state.loadingText)
            ErrorBanner(state.error, actions.onDismissError)
            when {
                offlineMode -> OfflineLibrary(state, query, bottomPadding, actions)
                state.loading && noData -> SkeletonList()
                noData && state.error == null -> EmptyState(hasQuery = query.isNotBlank())
                else -> TrackList(
                    state = state,
                    showMix = query.isBlank(),
                    bottomPadding = bottomPadding,
                    actions = actions
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
                    positionState = positionState, onToggle = actions.onTogglePlay,
                    onNext = actions.onNext, onOpen = actions.onOpenPlayer
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

// ───────────────────────── Offline library ─────────────────────────

/** Mode offline: ringkasan, unduhan berjalan, dan daftar lagu yang tersimpan (putar / hapus). */
@Composable
private fun OfflineLibrary(
    state: RexMusicUiState,
    query: String,
    bottomPadding: Dp,
    actions: LibraryActions
) {
    val offline = state.offline
    val q = query.trim()
    val shown = remember(offline.entries, q) {
        if (q.isEmpty()) offline.entries
        else offline.entries.filter {
            it.track.title.contains(q, ignoreCase = true) || it.track.artist.contains(q, ignoreCase = true)
        }
    }
    val nowId = state.nowPlaying?.id
    val loadingId = if (state.phase.isPlayerBusy) nowId else null
    val queuedIds = remember(state.userQueue) { state.userQueue.map { it.id }.toSet() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "off-hero") {
            OfflineHeroCard(
                count = offline.entries.size,
                bytes = offline.totalBytes,
                enabled = offline.entries.isNotEmpty(),
                onPlayAll = { actions.onPlayOfflineAll(false) },
                onShuffle = { actions.onPlayOfflineAll(true) }
            )
        }
        if (!offline.hasAccess) {
            item(key = "off-permission") { PermissionCard(actions.onRequestAccess) }
        }

        downloadsSection(offline.downloads, actions.onCancelDownload)

        if (offline.hasAccess && shown.isEmpty()) {
            item(key = "off-empty") { OfflineEmpty(hasQuery = q.isNotEmpty()) }
        } else if (shown.isNotEmpty()) {
            item(key = "off-header") {
                SectionHeader("Lagu Offline", shown.size, Modifier.padding(top = 14.dp, bottom = 2.dp))
            }
            itemsIndexed(shown, key = { _, e -> "o-${e.key}" }) { idx, entry ->
                val track = entry.track
                val active = track.id == nowId
                StaggerIn(index = idx, key = "o-${entry.key}") {
                    TrackCardContent(
                        title = track.title,
                        artist = "${track.artist} · ${formatBytes(entry.sizeBytes)}",
                        cover = track.cover,
                        onClick = { actions.onPlayOffline(track) },
                        overlay = overlayFor(active, active && state.isPlaying, track.id == loadingId),
                        active = active
                    ) {
                        QueueAddButton(track.id in queuedIds) { actions.onAddQueue(track) }
                        DeleteButton { actions.onDelete(track) }
                    }
                }
            }
            item(key = "off-delete-all") {
                TextButton(
                    onClick = actions.onDeleteAll,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Hapus semua unduhan")
                }
            }
        }
    }
}

/** Kartu besar mode offline: jumlah lagu, ukuran, lokasi folder, tombol Putar semua / Acak. */
@Composable
private fun OfflineHeroCard(
    count: Int,
    bytes: Long,
    enabled: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(scheme.tertiary, scheme.primary)))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.OfflinePin, contentDescription = null,
                    tint = scheme.onPrimary, modifier = Modifier.size(16.dp))
                Text("MODE OFFLINE", style = MaterialTheme.typography.labelSmall,
                    color = scheme.onPrimary.copy(alpha = 0.85f), letterSpacing = 1.5.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (count == 0) "Belum ada unduhan" else "$count lagu tersimpan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = scheme.onPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${formatBytes(bytes)} · ${RexOfflineManager.DISPLAY_PATH}",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onPrimary.copy(alpha = 0.85f),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroButton("Putar semua", Icons.Default.PlayArrow, filled = true, enabled = enabled,
                onClick = onPlayAll, modifier = Modifier.weight(1f))
            HeroButton("Acak", Icons.Default.Shuffle, filled = false, enabled = enabled,
                onClick = onShuffle, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroButton(
    label: String,
    icon: ImageVector,
    filled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val bg = if (filled) scheme.onPrimary else scheme.onPrimary.copy(alpha = 0.18f)
    val fg = if (filled) scheme.primary else scheme.onPrimary
    Row(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .clip(CircleShape)
            .background(bg)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

/** Ajakan memberi izin penyimpanan (tanpa izin, folder unduhan tidak bisa dibaca). */
@Composable
private fun PermissionCard(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = scheme.errorContainer.copy(alpha = 0.6f)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = scheme.onErrorContainer)
            Column(Modifier.weight(1f)) {
                Text("Izin penyimpanan dibutuhkan", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = scheme.onErrorContainer)
                Text("Ketuk untuk mengizinkan akses ke ${RexOfflineManager.DISPLAY_PATH}",
                    style = MaterialTheme.typography.bodySmall, color = scheme.onErrorContainer)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                tint = scheme.onErrorContainer)
        }
    }
}

@Composable
private fun OfflineEmpty(hasQuery: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(88.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CloudOff, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(if (hasQuery) "Tidak ada lagu yang cocok" else "Belum ada lagu offline",
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            if (hasQuery) "Coba kata kunci lain."
            else "Pindah ke mode Online, lalu tekan tombol unduh pada lagu yang kamu suka.",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Pintasan di mode online menuju perpustakaan offline. */
@Composable
private fun OfflineShortcutCard(count: Int, bytes: Long, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = scheme.tertiaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(scheme.tertiary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.OfflinePin, contentDescription = null, tint = scheme.tertiary)
            }
            Column(Modifier.weight(1f)) {
                Text("Lagu offline · $count lagu", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Text("${formatBytes(bytes)} · ketuk untuk membuka", style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Buka mode offline",
                tint = scheme.onSurfaceVariant)
        }
    }
}

/** Daftar unduhan yang sedang berjalan (dipakai di mode online dan offline). */
private fun LazyListScope.downloadsSection(
    downloads: Map<String, DownloadStatus>,
    onCancel: (RexTrack) -> Unit
) {
    if (downloads.isEmpty()) return
    item(key = "dl-header") {
        SectionHeader("Sedang Diunduh", downloads.size, Modifier.padding(top = 14.dp, bottom = 2.dp))
    }
    items(downloads.entries.toList(), key = { "dl-${it.key}" }) { entry ->
        DownloadRow(entry.value) { onCancel(entry.value.track) }
    }
}

@Composable
private fun DownloadRow(status: DownloadStatus, onCancel: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val label = when (status.stage) {
        DownloadStage.Queued -> "Menunggu giliran..."
        DownloadStage.Resolving -> "Menyiapkan..."
        DownloadStage.Downloading ->
            if (status.percent >= 0) "Mengunduh ${status.percent}%" else "Mengunduh..."
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = scheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CoverThumb(status.track.cover, 44.dp)
                Column(Modifier.weight(1f)) {
                    Text(status.track.title, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.primary)
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Batalkan unduhan")
                }
            }
            Spacer(Modifier.height(8.dp))
            if (status.stage == DownloadStage.Downloading && status.percent >= 0) {
                LinearProgressIndicator(
                    progress = status.percent / 100f,
                    modifier = Modifier.fillMaxWidth().clip(CircleShape)
                )
            } else {
                LinearProgressIndicator(Modifier.fillMaxWidth().clip(CircleShape))
            }
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

// ───────────────────────── Header, mode switch & search ─────────────────────────

/** Header custom dengan gradient tipis, sapaan, dan saklar Online / Offline. */
@Composable
private fun ScreenHeader(
    greeting: String,
    title: String,
    offline: Boolean,
    onSetOffline: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val accent by animateColorAsState(
        if (offline) scheme.tertiary else scheme.primary, tween(400), label = "headerAccent"
    )
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.18f), Color.Transparent)))
            .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Kembali")
        }
        Column(Modifier.weight(1f)) {
            Text(greeting, style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        ModeSwitch(offline, onSetOffline)
    }
}

/** Saklar dua segmen: Online / Offline. */
@Composable
private fun ModeSwitch(offline: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            .padding(3.dp)
    ) {
        ModeSegment("Online", Icons.Default.Cloud, selected = !offline) { onChange(false) }
        ModeSegment("Offline", Icons.Default.CloudOff, selected = offline) { onChange(true) }
    }
}

@Composable
private fun ModeSegment(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        if (selected) scheme.primary else Color.Transparent, label = "segBg"
    )
    val fg by animateColorAsState(
        if (selected) scheme.onPrimary else scheme.onSurfaceVariant, label = "segFg"
    )
    Row(
        Modifier.clip(CircleShape)
            .background(bg)
            .clickable(role = Role.Tab) { if (!selected) onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        Text(label, color = fg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Search bar pill dengan shadow halus dan tombol clear beranimasi. */
@Composable
private fun RexSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Cari lagu di Spotify"
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
            placeholder = { Text(placeholder) },
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

// ───────────────────────── Online list & sections ─────────────────────────

/** Satu LazyColumn: Mix, pintasan offline, unduhan, antrian, hasil pencarian, riwayat, rekomendasi. */
@Composable
private fun TrackList(
    state: RexMusicUiState,
    showMix: Boolean,
    bottomPadding: Dp,
    actions: LibraryActions
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
                MixHeroCard(state.favoriteArtists, state.popularCount, actions.onPlayMix)
            }
            if (state.offline.entries.isNotEmpty()) {
                item(key = "offline-shortcut") {
                    OfflineShortcutCard(
                        state.offline.entries.size, state.offline.totalBytes
                    ) { actions.onSetOffline(true) }
                }
            }
        }

        downloadsSection(state.offline.downloads, actions.onCancelDownload)

        if (state.userQueue.isNotEmpty()) {
            item(key = "queue") { QueueSummaryCard(state.userQueue, actions.onOpenQueue) }
        }

        trackSection(
            "Hasil Pencarian", "s", state.searchResults, nowId, state.isPlaying,
            loadingId, queuedIds, state.offline, actions, actions.onPlaySearch
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
                            onClick = { actions.onPlayHistory(idx) }
                        )
                    }
                }
            }
        }

        trackSection(
            "Rekomendasi", "m", state.tracks, nowId, state.isPlaying,
            loadingId, queuedIds, state.offline, actions, actions.onPlayMain
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
    offline: OfflineState,
    actions: LibraryActions,
    onTrackClick: (Int) -> Unit
) {
    if (tracks.isEmpty()) return
    item(key = "header-$keyPrefix") {
        SectionHeader(title, tracks.size, Modifier.padding(top = 14.dp, bottom = 2.dp))
    }
    itemsIndexed(tracks, key = { _, t -> "$keyPrefix-${t.id}" }) { idx, track ->
        val active = track.id == nowId
        val dlKey = remember(track.title, track.artist) { offlineKey(track) }
        StaggerIn(index = idx, key = "$keyPrefix-${track.id}") {
            TrackCard(
                track = track,
                active = active,
                playing = active && isPlaying,
                loading = track.id == loadingId,
                queued = track.id in queuedIds,
                downloaded = offline.isDownloaded(track, dlKey),
                status = offline.downloads[dlKey],
                onClick = { onTrackClick(idx) },
                onQueue = { actions.onAddQueue(track) },
                onDownload = { actions.onDownload(track) },
                onCancel = { actions.onCancelDownload(track) },
                onDelete = { actions.onDelete(track) }
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

/** Kartu besar untuk baris "Terakhir Diputar". Tap = putar ulang (file offline kalau ada, atau lewat API). */
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

/** Adapter dari RexTrack ke kartu stateless: tombol + antrian dan tombol unduh / batal / hapus. */
@Composable
private fun TrackCard(
    track: RexTrack,
    active: Boolean,
    playing: Boolean,
    loading: Boolean,
    queued: Boolean,
    downloaded: Boolean,
    status: DownloadStatus?,
    onClick: () -> Unit,
    onQueue: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    TrackCardContent(
        title = track.title,
        artist = track.artist,
        cover = track.cover,
        onClick = onClick,
        badge = if (downloaded) "Offline" else null,
        overlay = overlayFor(active, playing, loading),
        active = active
    ) {
        QueueAddButton(queued, onQueue)
        DownloadButton(downloaded, status, onDownload, onCancel, onDelete)
    }
}

/** Kartu lagu: cover (dengan overlay loading / equalizer), teks, dan slot tombol aksi di kanan. */
@Composable
private fun TrackCardContent(
    title: String,
    artist: String,
    cover: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    overlay: CoverOverlay = CoverOverlay.None,
    active: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
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
            CoverThumb(cover, overlay = overlay)
            TrackTexts(title, artist, badge, active, Modifier.weight(1f))
            actions()
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

/**
 * Tombol unduh dengan 3 keadaan:
 * - belum diunduh : ikon unduh (tap = mulai)
 * - mengunduh     : cincin progres + X (tap = batalkan)
 * - sudah diunduh : ikon centang unduhan (tap = hapus dari offline)
 */
@Composable
private fun DownloadButton(
    downloaded: Boolean,
    status: DownloadStatus?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val bg = if (downloaded) scheme.primary.copy(alpha = 0.16f) else scheme.onSurface.copy(alpha = 0.07f)
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(bg)
            .clickable(role = Role.Button) {
                when {
                    downloaded -> onDelete()
                    status != null -> onCancel()
                    else -> onDownload()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            status != null -> {
                if (status.stage == DownloadStage.Downloading && status.percent >= 0) {
                    CircularProgressIndicator(
                        progress = status.percent / 100f,
                        strokeWidth = 2.dp, color = scheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp, color = scheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Icon(Icons.Default.Close, contentDescription = "Batalkan unduhan",
                    tint = scheme.primary, modifier = Modifier.size(11.dp))
            }
            downloaded -> Icon(Icons.Default.DownloadDone, contentDescription = "Hapus dari offline",
                tint = scheme.primary, modifier = Modifier.size(18.dp))
            else -> Icon(Icons.Default.Download, contentDescription = "Unduh untuk offline",
                tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

/** Tombol hapus di perpustakaan offline. */
@Composable
private fun DeleteButton(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp).clip(CircleShape)
            .background(scheme.error.copy(alpha = 0.10f))
    ) {
        Icon(Icons.Default.Delete, contentDescription = "Hapus dari offline",
            tint = scheme.error, modifier = Modifier.size(18.dp))
    }
}

/** Thumbnail rounded 12dp; overlay gelap dengan spinner / equalizer / ikon play untuk lagu aktif. */
@Composable
private fun CoverThumb(cover: String, size: Dp = 56.dp, overlay: CoverOverlay = CoverOverlay.None) {
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
        if (overlay != CoverOverlay.None) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                when (overlay) {
                    CoverOverlay.Loading -> CircularProgressIndicator(
                        color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp)
                    )
                    CoverOverlay.Playing -> EqualizerBars(color = Color.White, height = 16.dp, barWidth = 3.dp)
                    CoverOverlay.Paused -> Icon(Icons.Default.PlayArrow, contentDescription = null,
                        tint = Color.White)
                    CoverOverlay.None -> Unit
                }
            }
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
            AutoplayToggleRow(
                state.autoplay, state.favoriteArtists, state.popularCount, state.offline.enabled, onAutoplay
            )

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
    offlineMode: Boolean,
    onToggle: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val subtitle = when {
        !autoplay -> "Mati: lagu berikutnya mengikuti urutan daftar"
        offlineMode -> "Lagu berikutnya dipilih dari unduhanmu"
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

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.2f GB".format(bytes / 1073741824.0)
    bytes >= 1L shl 20 -> "%.1f MB".format(bytes / 1048576.0)
    bytes >= 1L shl 10 -> "%d KB".format(bytes shr 10)
    else -> "$bytes B"
}

// ───────────────────────── Previews ─────────────────────────

@Preview(showBackground = true)
@Composable
private fun TrackCardPreview() {
    MaterialTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TrackCardContent("Judul Lagu Yang Cukup Panjang Sekali", "Nama Artis", "", {},
                badge = "Offline") {
                QueueAddButton(false) {}
                DownloadButton(downloaded = true, status = null, onDownload = {}, onCancel = {}, onDelete = {})
            }
            TrackCardContent("Sedang Mengunduh", "Nama Artis", "", {}) {
                QueueAddButton(false) {}
                DownloadButton(
                    downloaded = false,
                    status = DownloadStatus(
                        RexTrack("1", "Sedang Mengunduh", "Nama Artis"),
                        DownloadStage.Downloading, 45
                    ),
                    onDownload = {}, onCancel = {}, onDelete = {}
                )
            }
            TrackCardContent("Sedang Diputar", "Nama Artis", "", {},
                overlay = CoverOverlay.Playing, active = true) {
                QueueAddButton(true) {}
                DownloadButton(downloaded = false, status = null, onDownload = {}, onCancel = {}, onDelete = {})
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MixHeroPreview() {
    MaterialTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MixHeroCard(listOf("Tulus", "Hindia"), 30) {}
            OfflineHeroCard(count = 12, bytes = 54_300_000L, enabled = true, onPlayAll = {}, onShuffle = {})
            OfflineShortcutCard(12, 54_300_000L) {}
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HeaderPreview() {
    MaterialTheme {
        Column {
            ScreenHeader("Selamat malam", "RexMusic", offline = true, onSetOffline = {}, onBack = {})
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
