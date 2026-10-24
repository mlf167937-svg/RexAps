package com.rexaps.rexmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal fun artistPool(state: RexMusicUiState, limit: Int): List<String> =
    (state.favoriteArtists + RexPopularSongs.tracks.map { primaryArtist(it.artist) })
        .filter { it.isNotBlank() }
        .distinct()
        .take(limit)

//  HOME 

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

@Composable
internal fun ExploreTile(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
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

//  LIBRARY 
