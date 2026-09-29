package com.rexaps.rexmusic

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.rexaps.rexmusic.player.EqualizerBars
import com.rexaps.rexmusic.player.MusicPlayerCard
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
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
    var query by remember { mutableStateOf("") }
    val liked = remember { mutableStateMapOf<String, Boolean>() }

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

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad ->
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
                        onClose = { showPlayer = false }
                    )
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
                    onDismissError = { vm.dismissError() },
                    onTogglePlay = { vm.togglePlay() },
                    onNext = { vm.next() },
                    onOpenPlayer = { showPlayer = true },
                    onBack = onBack
                )
            }
        }
    }
}

// ───────────────────────── Pages ─────────────────────────

/** Halaman pemutar: top bar + kartu player + error + lagu sebelumnya/berikutnya. */
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
    onClose: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PlayerTopBar(onClose)
        MusicPlayerCard(
            track = track, isPlaying = state.isPlaying, positionMs = positionState.value,
            durationMs = state.durationMs, isLiked = isLiked, onPlayPause = onPlayPause,
            onNext = onNext, onPrev = onPrev, onSeek = onSeek, onToggleLike = onToggleLike,
            phase = state.phase, bufferedPercent = state.bufferedPercent,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        PlayerError(state.error, onRetry = onPlayPause)
        NeighborSection(state.previous, state.upNext, onOpenPrevious, onNext)
        Spacer(Modifier.height(24.dp))
    }
}

/** Top bar player: tombol tutup (panah bawah) + judul di tengah. */
@Composable
private fun PlayerTopBar(onClose: () -> Unit) {
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
        Spacer(Modifier.width(48.dp))
    }
}

/** Banner error di halaman player dengan tombol "Coba lagi" (putar ulang lewat API). */
@Composable
private fun PlayerError(error: String?, onRetry: () -> Unit) {
    AnimatedVisibility(visible = !error.isNullOrBlank()) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
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

/** Dua baris: lagu sebelumnya (dari riwayat) dan berikutnya (dari antrian). */
@Composable
private fun NeighborSection(
    previous: RexTrack?,
    upNext: RexTrack?,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    if (previous == null && upNext == null) return
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (previous != null) {
            NeighborRow("SEBELUMNYA", previous, Icons.Default.SkipPrevious, onPrevious)
        }
        if (upNext != null) {
            NeighborRow("SELANJUTNYA", upNext, Icons.Default.SkipNext, onNext)
        }
    }
}

@Composable
private fun NeighborRow(
    label: String,
    track: RexTrack,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
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
    onDismissError: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onOpenPlayer: () -> Unit,
    onBack: () -> Unit
) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val noData = state.searchResults.isEmpty() && state.tracks.isEmpty() && state.history.isEmpty()
    val loadingId = if (state.phase.isPlayerBusy) state.nowPlaying?.id else null
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
                    searchResults = state.searchResults,
                    history = state.history,
                    tracks = state.tracks,
                    nowId = nowPlaying?.id,
                    isPlaying = state.isPlaying,
                    loadingId = loadingId,
                    bottomPadding = if (nowPlaying != null) 112.dp else 32.dp,
                    onPlaySearch = onPlaySearch,
                    onPlayHistory = onPlayHistory,
                    onPlayMain = onPlayMain
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

/** Satu LazyColumn: hasil pencarian, riwayat (baris horizontal), lalu rekomendasi. */
@Composable
private fun TrackList(
    searchResults: List<RexTrack>,
    history: List<RexTrack>,
    tracks: List<RexTrack>,
    nowId: String?,
    isPlaying: Boolean,
    loadingId: String?,
    bottomPadding: Dp,
    onPlaySearch: (Int) -> Unit,
    onPlayHistory: (Int) -> Unit,
    onPlayMain: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        trackSection("Hasil Pencarian", "s", searchResults, nowId, isPlaying, loadingId, onPlaySearch)

        if (history.isNotEmpty()) {
            item(key = "header-h") {
                SectionHeader("Terakhir Diputar", history.size, Modifier.padding(top = 14.dp, bottom = 2.dp))
            }
            item(key = "row-h") {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(history, key = { _, t -> "h-${t.id}" }) { idx, track ->
                        val active = track.id == nowId
                        HistoryCard(
                            track = track, active = active, playing = active && isPlaying,
                            onClick = { onPlayHistory(idx) }
                        )
                    }
                }
            }
        }

        trackSection("Rekomendasi", "m", tracks, nowId, isPlaying, loadingId, onPlayMain)
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
    onTrackClick: (Int) -> Unit
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
                onClick = { onTrackClick(idx) }
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
    onClick: () -> Unit
) {
    TrackCardContent(
        title = track.title,
        artist = track.artist,
        cover = track.cover,
        onClick = onClick,
        loading = loading,
        active = active,
        playing = playing
    )
}

/** Kartu lagu: cover, teks, badge play/equalizer/spinner; highlight kalau sedang aktif. */
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
    playing: Boolean = false
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CoverThumb(cover)
            TrackTexts(title, artist, badge, active, Modifier.weight(1f))
            PlayBadgeButton(loading, active && playing)
        }
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
            TrackCardContent("Judul Lagu Yang Cukup Panjang Sekali", "Nama Artis", "", {}, badge = "Popular")
            TrackCardContent("Sedang Dimuat", "Nama Artis", "", {}, loading = true)
            TrackCardContent("Sedang Diputar", "Nama Artis", "", {}, active = true, playing = true)
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
