package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal fun artistPool(state: RexMusicUiState, limit: Int): List<String> =
    (state.favoriteArtists + RexPopularSongs.tracks.map { primaryArtist(it.artist) })
        .filter { it.isNotBlank() }
        .distinct()
        .take(limit)

internal fun LazyListScope.onlineHome(state: RexMusicUiState, actions: RexActions) {
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

internal fun buildQuickTiles(state: RexMusicUiState, actions: RexActions): List<QuickTile> {
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

internal fun LazyListScope.trackShelf(
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

internal fun LazyListScope.artistShelf(state: RexMusicUiState, actions: RexActions) {
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

internal fun LazyListScope.exploreSection(state: RexMusicUiState, actions: RexActions) {
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

internal fun LazyListScope.resultsSection(state: RexMusicUiState, actions: RexActions, typing: Boolean) {
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

internal fun LazyListScope.historySection(state: RexMusicUiState, actions: RexActions) {
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
