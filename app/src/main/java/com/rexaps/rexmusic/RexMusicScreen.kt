package com.rexaps.rexmusic

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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.rexaps.rexmusic.player.MusicPlayerCard
import java.util.Calendar
import kotlinx.coroutines.delay

// ───────────────────────── Screen (stateful entry point) ─────────────────────────

/** Layar utama RexMusic. Semua state UI lokal ada di sini, child cuma stateless. */
@Composable
fun RexMusicScreen(
    onBack: () -> Unit = {},
    vm: RexMusicViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    var showPlayer by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    // id lagu yang sedang loading/buffering, untuk spinner di list
    val loadingId = if (state.phase.isPlayerBusy) state.nowPlaying?.id else null

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
                        track = track, isPlaying = state.isPlaying,
                        positionMs = state.positionMs, durationMs = state.durationMs,
                        phase = state.phase, bufferedPercent = state.bufferedPercent,
                        isLiked = isLiked, onPlayPause = { vm.togglePlay() },
                        onNext = { vm.next() }, onPrev = { vm.prev() },
                        onSeek = { vm.seekTo(it) }, onToggleLike = { isLiked = !isLiked },
                        onClose = { showPlayer = false }
                    )
                }
            } else {
                BrowsePage(
                    query = query, loading = state.loading, loadingText = state.loadingText,
                    error = state.error, searchResults = state.searchResults, tracks = state.tracks,
                    loadingId = loadingId,
                    onQueryChange = {
                        query = it
                        if (it.isBlank()) vm.clearSearch() else vm.search(it)
                    },
                    onClear = { query = ""; vm.clearSearch() },
                    onPlaySearch = { vm.playFromSearch(it); showPlayer = true },
                    onPlayMain = { vm.playFromMain(it); showPlayer = true },
                    onBack = onBack
                )
            }
        }
    }
}

// ───────────────────────── Pages ─────────────────────────

/** Halaman pemutar: header + kartu player + tombol tutup. */
@Composable
private fun PlayerPage(
    track: RexTrack,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    phase: LoadPhase,
    bufferedPercent: Int,
    isLiked: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleLike: () -> Unit,
    onClose: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(greeting = "Sedang diputar", title = "RexMusic", onBack = onClose)
        MusicPlayerCard(
            track = track, isPlaying = isPlaying, positionMs = positionMs,
            durationMs = durationMs, isLiked = isLiked, onPlayPause = onPlayPause,
            onNext = onNext, onPrev = onPrev, onSeek = onSeek, onToggleLike = onToggleLike,
            phase = phase, bufferedPercent = bufferedPercent,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Tutup player")
        }
    }
}

/** Halaman browse: header, search, status (loading/error), lalu list/empty/skeleton. */
@Composable
private fun BrowsePage(
    query: String,
    loading: Boolean,
    loadingText: String,
    error: String?,
    searchResults: List<RexTrack>,
    tracks: List<RexTrack>,
    loadingId: String?,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onPlaySearch: (Int) -> Unit,
    onPlayMain: (Int) -> Unit,
    onBack: () -> Unit
) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val noData = searchResults.isEmpty() && tracks.isEmpty()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(greetingForHour(hour), "RexMusic", onBack)
        RexSearchBar(query, onQueryChange, onClear, Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        LoadingHint(loading, loadingText)
        ErrorBanner(error)
        when {
            loading && noData -> SkeletonList()
            noData && error == null -> EmptyState(hasQuery = query.isNotBlank())
            else -> TrackList(searchResults, tracks, loadingId, onPlaySearch, onPlayMain)
        }
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

/** Banner error ringkas, muncul hanya kalau ada pesan. */
@Composable
private fun ErrorBanner(error: String?) {
    AnimatedVisibility(visible = !error.isNullOrBlank()) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.width(8.dp))
                Text("Terjadi masalah: ${error.orEmpty()}",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall)
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

/** Satu LazyColumn untuk dua section (tanpa nested lazy). */
@Composable
private fun TrackList(
    searchResults: List<RexTrack>,
    tracks: List<RexTrack>,
    loadingId: String?,
    onPlaySearch: (Int) -> Unit,
    onPlayMain: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        trackSection("Hasil Pencarian", "s", searchResults, loadingId, onPlaySearch)
        trackSection("Rekomendasi", "m", tracks, loadingId, onPlayMain)
    }
}

/** Menambah header + item lagu ke LazyColumn. Key tetap "s-id" / "m-id". */
private fun LazyListScope.trackSection(
    title: String,
    keyPrefix: String,
    tracks: List<RexTrack>,
    loadingId: String?,
    onTrackClick: (Int) -> Unit
) {
    if (tracks.isEmpty()) return
    item(key = "header-$keyPrefix") {
        SectionHeader(title, tracks.size, Modifier.padding(top = 14.dp, bottom = 2.dp))
    }
    itemsIndexed(tracks, key = { _, t -> "$keyPrefix-${t.id}" }) { idx, track ->
        StaggerIn(index = idx, key = "$keyPrefix-${track.id}") {
            TrackCard(
                track = track,
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

// ───────────────────────── Track card ─────────────────────────

/** Adapter dari RexTrack ke kartu stateless (supaya gampang di-preview). */
@Composable
private fun TrackCard(track: RexTrack, loading: Boolean, onClick: () -> Unit) {
    TrackCardContent(
        title = track.title,
        artist = track.artist,
        cover = track.cover,
        onClick = onClick,
        loading = loading
    )
}

/** Kartu lagu: cover, teks, tombol play/spinner; ada ripple + scale saat ditekan. */
@Composable
private fun TrackCardContent(
    title: String,
    artist: String,
    cover: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    loading: Boolean = false
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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(Color.Transparent, primary.copy(alpha = 0.06f))))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CoverThumb(cover)
            TrackTexts(title, artist, badge, Modifier.weight(1f))
            PlayBadgeButton(loading)
        }
    }
}

/** Thumbnail 56dp rounded 12dp; fallback ikon kalau cover kosong. */
@Composable
private fun CoverThumb(cover: String) {
    Box(
        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
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
private fun TrackTexts(title: String, artist: String, badge: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
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

/** Lingkaran kecil: ikon play, atau spinner kalau lagu ini sedang dimuat. */
@Composable
private fun PlayBadgeButton(loading: Boolean) {
    Box(
        modifier = Modifier.size(36.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp, modifier = Modifier.size(18.dp),
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(Icons.Default.PlayArrow, contentDescription = "Putar",
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
