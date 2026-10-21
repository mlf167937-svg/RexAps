package com.rexaps.rexmusic

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

//  Shared row helper 

@Composable
private fun TrackRowFor(
    track: RexTrack,
    state: RexMusicUiState,
    actions: RexActions,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val key = remember(track.title, track.artist) { offlineKey(track) }
    val active = track.id == state.nowPlaying?.id
    TrackRow(
        track = track,
        active = active,
        playing = active && state.isPlaying,
        loading = active && state.phase.isPlayerBusy,
        downloaded = state.offline.isDownloaded(track, key),
        status = state.offline.downloads[key],
        subtitle = subtitle,
        onClick = onClick,
        onMore = { actions.openMenu(MenuTarget(track, onClick)) }
    )
}

private fun artistPool(state: RexMusicUiState, limit: Int): List<String> =
    (state.favoriteArtists + RexPopularSongs.tracks.map { primaryArtist(it.artist) })
        .filter { it.isNotBlank() }
        .distinct()
        .take(limit)

//  HOME 

@Composable
fun HomeTab(
    state: RexMusicUiState,
    actions: RexActions,
    listState: LazyListState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val offlineMode = state.offline.enabled
    val glow by animateColorAsState(
        if (offlineMode) scheme.tertiary else scheme.primary, tween(400), label = "homeGlow"
    )
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }

    Box(
        modifier.fillMaxSize().drawBehind {
            val h = 320.dp.toPx()
            drawRect(
                Brush.verticalGradient(
                    listOf(glow.copy(alpha = 0.30f), Color.Transparent), startY = 0f, endY = h
                ),
                size = Size(size.width, h)
            )
        }
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "home-header") {
                HomeHeader(
                    greeting = greetingForHour(hour),
                    offline = offlineMode,
                    onToggle = { actions.setOffline(!offlineMode) },
                    onBack = onBack
                )
            }
            if (offlineMode) {
                offlineSection(state, actions, query = "", full = true)
            } else {
                onlineHome(state, actions)
            }
        }
    }
}

@Composable
private fun HomeHeader(greeting: String, offline: Boolean, onToggle: () -> Unit, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RexIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", onBack, size = 48.dp)
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(
                greeting, style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                "RexMusic", style = MaterialTheme.typography.headlineSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        ModeChip(offline, onToggle)
    }
}

private fun LazyListScope.onlineHome(state: RexMusicUiState, actions: RexActions) {
    val nowId = state.nowPlaying?.id
    val tiles = buildQuickTiles(state, actions)
    if (tiles.isNotEmpty()) {
        item(key = "home-grid") {
            QuickGrid(tiles, nowId, state.isPlaying, Modifier.padding(top = 8.dp))
        }
    }
    item(key = "home-mix") {
        MixHeroCard(
            state.favoriteArtists, state.popularCount, actions.playMix,
            Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )
    }
    downloadsSection(state.offline.downloads, actions.cancelDownload)
    trackShelf("hist", "Terakhir diputar", state.history, state, actions.playHistory)
    trackShelf("rec", "Rekomendasi untukmu", state.tracks, state, actions.playMain)
    artistShelf(state, actions)
}

private fun buildQuickTiles(state: RexMusicUiState, actions: RexActions): List<QuickTile> {
    val tiles = mutableListOf<QuickTile>()
    if (state.offline.entries.isNotEmpty()) {
        tiles += QuickTile("special-offline", "Lagu Offline", "", special = true) { actions.goLibrary() }
    }
    if (state.playlists.isNotEmpty()) {
        tiles += QuickTile("special-playlist", "Playlist", "", special = true) { actions.goPlaylists() }
    }
    state.history.take(6).forEachIndexed { i, t ->
        tiles += QuickTile(t.id, t.title, t.cover) { actions.playHistory(i) }
    }
    state.tracks.forEachIndexed { i, t ->
        if (tiles.none { tile -> tile.id == t.id }) {
            tiles += QuickTile(t.id, t.title, t.cover) { actions.playMain(i) }
        }
    }
    return tiles.take(6)
}

private fun LazyListScope.trackShelf(
    key: String,
    title: String,
    tracks: List<RexTrack>,
    state: RexMusicUiState,
    onPlay: (Int) -> Unit
) {
    if (tracks.isEmpty()) return
    item(key = "$key-title") { SectionTitle(title) }
    item(key = "$key-row") {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(tracks, key = { _, t -> "$key-${t.id}" }) { idx, t ->
                val active = t.id == state.nowPlaying?.id
                ShelfCard(t, active, active && state.isPlaying, onClick = { onPlay(idx) })
            }
        }
    }
}

private fun LazyListScope.artistShelf(state: RexMusicUiState, actions: RexActions) {
    val artists = artistPool(state, 12)
    if (artists.isEmpty()) return
    item(key = "artist-title") {
        SectionTitle(if (state.favoriteArtists.isNotEmpty()) "Artis favoritmu" else "Artis populer")
    }
    item(key = "artist-row") {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(artists, key = { it }) { name ->
                ArtistCard(name, onClick = { actions.searchArtist(name) })
            }
        }
    }
}

@Composable
private fun MixHeroCard(
    favorites: List<String>,
    popularCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtitle = when {
        favorites.isNotEmpty() -> "Berdasarkan ${favorites.joinToString(", ")}"
        popularCount > 0 -> "Acak dari $popularCount lagu populer"
        else -> "Putar beberapa lagu, Mix akan belajar seleramu"
    }
    val source = remember { MutableInteractionSource() }
    val scale = rememberPressScale(source, 0.98f)
    Box(
        modifier.fillMaxWidth()
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(RoundedCornerShape(20.dp))
            .background(RexHeroBrush)
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick)
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "MIX UNTUKMU", style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f), letterSpacing = 1.5.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Putar Mix Pintar", style = MaterialTheme.typography.headlineSmall,
                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(16.dp))
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow, contentDescription = "Putar Mix",
                    tint = RexPlayerBase, modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

//  SEARCH 

@Composable
fun SearchTab(
    state: RexMusicUiState,
    query: String,
    typing: Boolean,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    actions: RexActions,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val offlineMode = state.offline.enabled
    val q = query.trim()
    LazyColumn(
        modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "search-header") {
            TabHeader("Cari", offlineMode) { actions.setOffline(!offlineMode) }
        }
        item(key = "search-field") {
            SearchField(
                query, onQueryChange, onClear,
                if (offlineMode) "Cari di lagu offline" else "Lagu, artis, atau album",
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        when {
            offlineMode -> offlineSection(state, actions, q, full = false)
            q.isEmpty() -> exploreSection(state, actions)
            else -> resultsSection(state, actions, typing)
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                RexIconButton(
                    Icons.Default.Clear, "Hapus pencarian", onClear,
                    size = 40.dp, iconSize = 20.dp, tint = scheme.onSurfaceVariant
                )
            }
        },
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = scheme.surfaceVariant,
            unfocusedContainerColor = scheme.surfaceVariant,
            disabledContainerColor = scheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = scheme.primary
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        modifier = modifier.fillMaxWidth()
    )
}

private fun LazyListScope.exploreSection(state: RexMusicUiState, actions: RexActions) {
    val artists = artistPool(state, 20)
    item(key = "explore-title") { SectionTitle("Jelajahi artis") }
    items(artists.chunked(2), key = { it.first() }) { pair ->
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            pair.forEach { name ->
                ExploreTile(name, { actions.searchArtist(name) }, Modifier.weight(1f))
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ExploreTile(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = rexTileColor(name)
    Box(
        modifier.height(88.dp).clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(c, lerp(c, Color.Black, 0.35f))))
            .clickable(onClick = onClick)
    ) {
        Text(
            name, style = MaterialTheme.typography.titleMedium, color = Color.White,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 12.dp, end = 48.dp)
        )
        Icon(
            Icons.Default.MusicNote, contentDescription = null,
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 8.dp, y = 8.dp)
                .size(56.dp).rotate(20f)
        )
    }
}

private fun LazyListScope.resultsSection(state: RexMusicUiState, actions: RexActions, typing: Boolean) {
    val results = state.searchResults
    when {
        results.isEmpty() && (state.loading || typing) ->
            item(key = "sr-skeleton") { SkeletonList() }
        results.isEmpty() && state.error == null ->
            item(key = "sr-empty") {
                EmptyState(
                    Icons.Default.Search, "Lagu tidak ditemukan",
                    "Coba kata kunci lain, misalnya judul lagu atau nama artis."
                )
            }
        results.isEmpty() -> Unit
        else -> {
            item(key = "sr-title") { SectionTitle("Hasil pencarian") }
            itemsIndexed(results, key = { _, t -> "sr-${t.id}" }) { idx, track ->
                TrackRowFor(track, state, actions) { actions.playSearch(idx) }
            }
        }
    }
}

//  LIBRARY 

@Composable
fun LibraryTab(
    state: RexMusicUiState,
    libTab: Int,
    onLibTab: (Int) -> Unit,
    actions: RexActions,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val offlineMode = state.offline.enabled
    LazyColumn(
        modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "lib-header") {
            TabHeader("Koleksi", offlineMode) { actions.setOffline(!offlineMode) }
        }
        item(key = "lib-chips") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibraryChip("Unduhan", state.offline.entries.size, libTab == 0) { onLibTab(0) }
                LibraryChip("Antrian", state.userQueue.size, libTab == 1) { onLibTab(1) }
                LibraryChip("Riwayat", state.history.size, libTab == 2) { onLibTab(2) }
            }
        }
        when (libTab) {
            0 -> offlineSection(state, actions, query = "", full = true)
            1 -> queueSection(state, actions, showNow = false)
            else -> historySection(state, actions)
        }
    }
}

@Composable
fun PlaylistTab(
    state: RexMusicUiState,
    actions: RexActions,
    listState: LazyListState,
    selectedPlaylistName: String?,
    onOpenPlaylist: (String) -> Unit,
    onBackFromDetail: () -> Unit,
    onRequestDeletePlaylist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreate by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    var playlistQuery by rememberSaveable { mutableStateOf("") }
    val detailListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val selectedPlaylist = state.playlists.firstOrNull { it.name == selectedPlaylistName }

    LaunchedEffect(selectedPlaylistName) {
        if (selectedPlaylistName != null) detailListState.scrollToItem(0)
    }
    LaunchedEffect(selectedPlaylistName, state.playlists) {
        if (selectedPlaylistName != null && selectedPlaylist == null) onBackFromDetail()
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 1120.dp -> 3
            maxWidth >= 650.dp -> 2
            else -> 1
        }
        val wideDetail = maxWidth >= 720.dp

        if (selectedPlaylist != null) {
            PlaylistDetailScreen(
                playlist = selectedPlaylist,
                state = state,
                actions = actions,
                listState = detailListState,
                wide = wideDetail,
                onBack = onBackFromDetail,
                onDelete = { onRequestDeletePlaylist(selectedPlaylist.name) }
            )
        } else {
            val filteredPlaylists = remember(state.playlists, playlistQuery) {
                val query = playlistQuery.trim()
                if (query.isBlank()) state.playlists
                else state.playlists.filter { playlist ->
                    playlist.name.contains(query, ignoreCase = true) ||
                        playlist.tracks.any { it.title.contains(query, true) || it.artist.contains(query, true) }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item(key = "playlist-page-header") {
                    Column(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "KOLEKSI MUSIK",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.2.sp
                                )
                                Text("Playlist", style = MaterialTheme.typography.headlineLarge)
                            }
                            FilledTonalButton(
                                onClick = { showCreate = true },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(19.dp))
                                Spacer(Modifier.width(7.dp))
                                Text("Buat playlist")
                            }
                        }
                        Text(
                            "Susun lagu sesuai suasana, aktivitas, atau momen favoritmu.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${state.playlists.size} playlist · tersimpan di perangkat",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item(key = "playlist-search") {
                    OutlinedTextField(
                        value = playlistQuery,
                        onValueChange = { playlistQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (playlistQuery.isNotBlank()) {
                                IconButton(onClick = { playlistQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Hapus pencarian")
                                }
                            }
                        },
                        placeholder = { Text("Cari playlist atau lagu di dalamnya") }
                    )
                }

                if (state.playlists.isEmpty()) {
                    item(key = "playlist-empty") {
                        PlaylistEmptyState(onCreate = { showCreate = true })
                    }
                } else if (filteredPlaylists.isEmpty()) {
                    item(key = "playlist-no-search-results") {
                        EmptyState(
                            Icons.Default.Search,
                            "Playlist tidak ditemukan",
                            "Coba kata kunci lain atau hapus pencarian."
                        )
                    }
                } else {
                    item(key = "playlist-list-heading") {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Koleksi kamu", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                            Text("${filteredPlaylists.size} item", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(
                        items = filteredPlaylists.chunked(columns),
                        key = { row -> "playlist-row-${row.firstOrNull()?.name.orEmpty()}" }
                    ) { rowPlaylists ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            rowPlaylists.forEach { playlist ->
                                PlaylistGridCard(
                                    playlist = playlist,
                                    compact = columns == 1,
                                    onOpen = { onOpenPlaylist(playlist.name) },
                                    onPlay = { actions.playPlaylist(playlist.name) },
                                    onDelete = { onRequestDeletePlaylist(playlist.name) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat((columns - rowPlaylists.size).coerceAtLeast(0)) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            icon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
            title = { Text("Buat playlist") },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    singleLine = true,
                    label = { Text("Nama playlist") },
                    placeholder = { Text("Santai, Workout, Favorit...") }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = playlistName.trim().isNotEmpty(),
                    onClick = {
                        actions.createPlaylist(playlistName.trim())
                        playlistName = ""
                        showCreate = false
                    }
                ) { Text("Buat playlist") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Batal") } }
        )
    }
}

@Composable
private fun PlaylistGridCard(
    playlist: RexPlaylist,
    compact: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = scheme.surfaceVariant),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        if (compact) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaylistArtwork(playlist, Modifier.size(88.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(playlist.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${playlist.tracks.size} lagu",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        if (playlist.tracks.isEmpty()) "Playlist kosong" else playlist.tracks.take(2).joinToString(" · ") { it.title },
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onPlay, enabled = playlist.tracks.isNotEmpty()) {
                        Icon(Icons.Default.PlayCircle, contentDescription = "Putar ${playlist.name}", tint = scheme.primary, modifier = Modifier.size(30.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus playlist ${playlist.name}", tint = scheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(12.dp)) {
                    PlaylistArtwork(playlist, Modifier.fillMaxWidth().aspectRatio(1.12f))
                    Box(
                        Modifier.align(Alignment.BottomEnd).padding(22.dp).size(48.dp)
                            .clip(CircleShape).background(scheme.primary)
                            .clickable(enabled = playlist.tracks.isNotEmpty(), onClick = onPlay),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Putar ${playlist.name}", tint = scheme.onPrimary, modifier = Modifier.size(28.dp))
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f).clickable(onClick = onOpen), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(playlist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (playlist.tracks.isEmpty()) "Playlist kosong" else "${playlist.tracks.size} lagu",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus playlist ${playlist.name}", tint = scheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistArtwork(
    playlist: RexPlaylist,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val tracks = playlist.tracks.take(4)
    Box(
        modifier.clip(RoundedCornerShape(17.dp)).background(scheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            tracks.isEmpty() -> {
                Box(Modifier.fillMaxSize().background(RexHeroBrush), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.QueueMusic, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                }
            }
            tracks.size == 1 -> CoverImage(
                tracks.first().cover,
                Modifier.fillMaxSize(),
                shape = RoundedCornerShape(17.dp),
                contentDescription = "Sampul ${tracks.first().title}"
            )
            else -> {
                Column(
                    Modifier.fillMaxSize().padding(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        PlaylistArtworkTile(tracks.getOrNull(0), Modifier.weight(1f).fillMaxHeight())
                        PlaylistArtworkTile(tracks.getOrNull(1), Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        PlaylistArtworkTile(tracks.getOrNull(2), Modifier.weight(1f).fillMaxHeight())
                        PlaylistArtworkTile(tracks.getOrNull(3), Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistArtworkTile(track: RexTrack?, modifier: Modifier = Modifier) {
    if (track == null) {
        Box(
            modifier.clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    } else {
        CoverImage(
            track.cover,
            modifier,
            shape = RoundedCornerShape(5.dp),
            contentDescription = "Sampul ${track.title}"
        )
    }
}

@Composable
private fun PlaylistDetailScreen(
    playlist: RexPlaylist,
    state: RexMusicUiState,
    actions: RexActions,
    listState: LazyListState,
    wide: Boolean,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item(key = "playlist-detail-toolbar") {
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali ke playlist")
                }
                Text("Detail playlist", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus playlist", tint = scheme.onSurfaceVariant)
                }
            }
        }
        item(key = "playlist-detail-hero") {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                val detailArtworkSize = minOf(maxWidth * 0.31f, 236.dp)
                if (wide) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                            .background(scheme.surfaceVariant).padding(22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        PlaylistArtwork(playlist, Modifier.size(detailArtworkSize))
                        PlaylistDetailText(playlist, onPlay = { actions.playPlaylist(playlist.name) }, modifier = Modifier.weight(1f))
                    }
                } else {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                            .background(scheme.surfaceVariant).padding(18.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            PlaylistArtwork(playlist, Modifier.fillMaxWidth(0.76f).aspectRatio(1f))
                        }
                        PlaylistDetailText(playlist, onPlay = { actions.playPlaylist(playlist.name) }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        item(key = "playlist-detail-list-heading") {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Lagu di playlist", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${playlist.tracks.size} lagu · pilih lagu untuk mulai mendengarkan",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                if (playlist.tracks.isNotEmpty()) {
                    TextButton(onClick = { actions.playPlaylist(playlist.name) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Putar semua")
                    }
                }
            }
        }
        if (playlist.tracks.isEmpty()) {
            item(key = "playlist-detail-empty") {
                EmptyState(
                    Icons.Default.QueueMusic,
                    "Playlist masih kosong",
                    "Tambahkan lagu lewat menu ⋮ pada lagu di Beranda, Cari, atau Koleksi."
                )
            }
        } else {
            itemsIndexed(playlist.tracks, key = { index, track -> "detail-${playlist.name}-${track.id}-$index" }) { _, track ->
                TrackRowFor(track, state, actions) {
                    actions.playPlaylistTrack(playlist.name, track.id)
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailText(
    playlist: RexPlaylist,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "REX MUSIC · PLAYLIST PRIBADI",
            style = MaterialTheme.typography.labelMedium,
            color = scheme.primary,
            letterSpacing = 1.1.sp
        )
        Text(
            playlist.name,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "${playlist.tracks.size} lagu · koleksi pribadimu",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant
        )
        Text(
            if (playlist.tracks.isEmpty()) "Mulai bangun playlist ini dengan lagu favoritmu." else playlist.tracks.take(3).joinToString("  ·  ") { it.title },
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Button(
            onClick = onPlay,
            enabled = playlist.tracks.isNotEmpty(),
            shape = RoundedCornerShape(15.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Putar playlist")
        }
    }
}

@Composable
private fun PlaylistEmptyState(onCreate: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp)
            .clip(RoundedCornerShape(24.dp)).background(scheme.surfaceVariant).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(76.dp).clip(RoundedCornerShape(24.dp)).background(RexHeroBrush),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.QueueMusic, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
        }
        Text("Mulai koleksi musikmu", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            "Buat playlist untuk mengelompokkan lagu sesuai mood. Sampul playlist akan terbentuk otomatis dari thumbnail lagu di dalamnya.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onCreate, shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Buat playlist pertama")
        }
    }
}


private fun LazyListScope.historySection(state: RexMusicUiState, actions: RexActions) {
    if (state.history.isEmpty()) {
        item(key = "hist-empty") {
            EmptyState(
                Icons.Default.History, "Belum ada riwayat",
                "Lagu yang kamu putar akan muncul di sini."
            )
        }
        return
    }
    item(key = "hist-title") {
        SectionTitle(
            "Terakhir diputar",
            trailing = { TextButton(onClick = actions.clearHistory) { Text("Hapus") } }
        )
    }
    itemsIndexed(state.history, key = { _, t -> "hist-${t.id}" }) { idx, track ->
        TrackRowFor(track, state, actions) { actions.playHistory(idx) }
    }
}

//  OFFLINE (dipakai Home offline, Cari offline, Koleksi) 

fun LazyListScope.offlineSection(
    state: RexMusicUiState,
    actions: RexActions,
    query: String,
    full: Boolean
) {
    val offline = state.offline
    val q = query.trim()
    val shown = if (q.isEmpty()) offline.entries else offline.entries.filter {
        it.track.title.contains(q, ignoreCase = true) || it.track.artist.contains(q, ignoreCase = true)
    }

    if (full) {
        item(key = "off-hero") {
            OfflineHeroCard(
                count = offline.entries.size,
                bytes = offline.totalBytes,
                enabled = offline.entries.isNotEmpty(),
                onPlayAll = { actions.playOfflineAll(false) },
                onShuffle = { actions.playOfflineAll(true) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        if (!offline.hasAccess) {
            item(key = "off-permission") {
                PermissionCard(actions.requestAccess, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }
    }

    downloadsSection(offline.downloads, actions.cancelDownload)

    if (shown.isEmpty()) {
        item(key = "off-empty") {
            EmptyState(
                Icons.Default.CloudOff,
                if (q.isNotEmpty()) "Tidak ada lagu yang cocok" else "Belum ada lagu offline",
                if (q.isNotEmpty()) "Coba kata kunci lain."
                else "Pindah ke mode Online, lalu buka menu  pada lagu dan pilih Unduh."
            )
        }
    } else {
        item(key = "off-title") {
            SectionTitle(if (full) "Lagu offline  ${shown.size}" else "Hasil offline  ${shown.size}")
        }
        items(shown, key = { "o-${it.key}" }) { entry ->
            TrackRowFor(
                entry.track, state, actions,
                subtitle = "${entry.track.artist}  ${formatBytes(entry.sizeBytes)}"
            ) { actions.playOffline(entry.track) }
        }
        if (full) {
            item(key = "off-delete-all") {
                TextButton(
                    onClick = actions.deleteAll,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Hapus semua unduhan")
                }
            }
        }
    }
}

fun LazyListScope.downloadsSection(
    downloads: Map<String, DownloadStatus>,
    onCancel: (RexTrack) -> Unit
) {
    if (downloads.isEmpty()) return
    item(key = "dl-title") { SectionTitle("Sedang diunduh  ${downloads.size}") }
    items(downloads.entries.toList(), key = { "dl-${it.key}" }) { entry ->
        DownloadRow(entry.value) { onCancel(entry.value.track) }
    }
}

@Composable
private fun OfflineHeroCard(
    count: Int,
    bytes: Long,
    enabled: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(RexOfflineBrush).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.OfflinePin, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(16.dp)
                )
                Text(
                    "MODE OFFLINE", style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f), letterSpacing = 1.5.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (count == 0) "Belum ada unduhan" else "$count lagu tersimpan",
                style = MaterialTheme.typography.headlineSmall, color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${formatBytes(bytes)}  ${RexOfflineManager.DISPLAY_PATH}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.88f),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroButton("Putar semua", Icons.Default.PlayArrow, true, enabled, onPlayAll, Modifier.weight(1f))
            HeroButton("Acak", Icons.Default.Shuffle, false, enabled, onShuffle, Modifier.weight(1f))
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
    val bg = if (filled) Color.White else Color.White.copy(alpha = 0.2f)
    val fg = if (filled) RexPlayerBase else Color.White
    Row(
        modifier.height(44.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .clip(CircleShape).background(bg)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun PermissionCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(scheme.errorContainer.copy(alpha = 0.7f))
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Folder, contentDescription = null, tint = scheme.onErrorContainer)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Izin penyimpanan dibutuhkan", style = MaterialTheme.typography.titleSmall,
                color = scheme.onErrorContainer
            )
            Text(
                "Ketuk untuk mengizinkan akses ke ${RexOfflineManager.DISPLAY_PATH}",
                style = MaterialTheme.typography.bodySmall, color = scheme.onErrorContainer
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = scheme.onErrorContainer)
    }
}

@Composable
private fun LibraryChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Box(
        Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(
                if (selected) scheme.primary
                else scheme.surfaceVariant
            )
            .clickable(
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 0) "$label $count" else label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) scheme.onPrimary
            else scheme.onSurface,
            maxLines = 1
        )
    }
}
