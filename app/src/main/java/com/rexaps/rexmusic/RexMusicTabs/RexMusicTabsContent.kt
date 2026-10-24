package com.rexaps.rexmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
internal fun TrackRowFor(
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

@Composable
internal fun PermissionCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
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
