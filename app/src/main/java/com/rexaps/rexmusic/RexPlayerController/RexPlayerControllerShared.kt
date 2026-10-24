package com.rexaps.rexmusic

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun RexPlayerController.createPlaylistImpl(name: String) {
    if (playlistStore.create(name) == null) {
        postNotice("Nama playlist tidak valid atau sudah ada")
        return
    }
    refreshPlaylists()
    postNotice("Playlist dibuat: ${name.trim()}")
}

internal fun RexPlayerController.addToPlaylistImpl(name: String, track: RexTrack) {
    if (playlistStore.addTrack(name, track)) {
        refreshPlaylists()
        postNotice("Ditambahkan ke $name")
    } else {
        postNotice("Playlist tidak bisa diperbarui")
    }
}

internal fun RexPlayerController.playPlaylistImpl(name: String, startTrackId: String? = null) {
    val playlist = playlistStore.load(name) ?: return
    val tracks = if (offlineMode()) playlist.tracks.filter { RexOfflineManager.isDownloaded(it) } else playlist.tracks
    if (tracks.isEmpty()) {
        postNotice(if (offlineMode()) "Playlist ini belum punya lagu offline" else "Playlist masih kosong")
        return
    }
    // When a track is tapped inside playlist details, start at that track while
    // keeping the playlist as the active playback context for Next / Previous.
    val startIndex = if (startTrackId == null) 0 else tracks.indexOfFirst { it.id == startTrackId }
    if (startIndex < 0) {
        postNotice(if (offlineMode()) "Lagu ini belum tersedia offline" else "Lagu tidak ditemukan di playlist")
        return
    }
    startQueue(tracks, startIndex, follow = true)
}

//  Search 

/** Debounced: aman dipanggil di tiap ketikan. Tidak dipakai saat mode offline. */
internal fun RexPlayerController.searchImpl(query: String) {
    searchJob?.cancel()
    if (offlineMode()) return
    val q = query.trim()
    if (q.isEmpty()) {
        clearSearch()
        return
    }
    searchJob = scope.launch {
        delay(SEARCH_DEBOUNCE_MS)
        searching = true
        _state.update { it.copy(error = null) }
        publishLoading()
        RexMusicApi.search(q)
            .onSuccess { list ->
                searching = false
                _state.update { it.copy(searchResults = list) }
            }
            .onFailure { e ->
                searching = false
                _state.update { it.copy(error = e.message ?: "search gagal") }
            }
        publishLoading()
    }
}

internal const val SEARCH_DEBOUNCE_MS = 400L

internal const val SEEK_DEBOUNCE_MS = 80L

internal const val PROGRESS_INTERVAL_MS = 100L

internal const val RESTART_THRESHOLD_MS = 3000L

internal const val STALL_TICKS = 6

internal const val MAX_HISTORY = 50

internal const val MAX_QUEUE = 100

internal const val MAX_REPEAT = 50

// smart autoplay (silakan ubah sesuai selera)
internal const val TASTE_CHANCE = 0.65f        // peluang lagu berikutnya dari artis favorit

internal const val TASTE_LISTEN_MS = 15_000L   // didengar segini lama = dihitung "suka"

internal const val EARLY_SKIP_MS = 10_000L     // di-skip sebelum ini = dianggap kurang suka

internal const val REPEAT_TASTE_WEIGHT = 0.5

internal const val RECENT_WINDOW = 8           // hindari lagu yang baru diputar

internal const val SMART_RETRIES = 3
