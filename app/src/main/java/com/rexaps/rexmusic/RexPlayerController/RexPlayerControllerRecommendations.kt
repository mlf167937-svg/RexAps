package com.rexaps.rexmusic

import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Debounced: aman dipanggil di tiap ketikan. Tidak dipakai saat mode offline. */
    fun RexPlayerController.__split_search_8(query: String) {
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

/**
     * File offline dipakai kalau ada (tanpa internet). Kalau belum diunduh: saat mode offline gagal
     * dengan pesan jelas, saat online diambil lewat API. true = pemutaran dimulai.
     */
    internal suspend fun RexPlayerController.__split_resolveAndStart_16(track: RexTrack): Boolean {
        val local = RexOfflineManager.audioFile(track)
        if (local != null) {
            resolving = false
            _state.update { it.copy(nowPlayingOffline = true) }
            publishLoading()
            startPlayer(local.absolutePath, local = true)
            return true
        }
        if (offlineMode()) {
            resolving = false
            _state.update { it.copy(error = "Mode offline: \"${track.title}\" belum diunduh") }
            publishLoading()
            return false
        }

        val result = RexTrackResolver.resolve(track)
        resolving = false
        val fresh = result.getOrNull()
        if (fresh == null) {
            _state.update { it.copy(error = result.exceptionOrNull()?.message ?: "gagal memuat audio") }
            publishLoading()
            return false
        }
        replaceTrack(fresh)
        publishLoading()
        startPlayer(fresh.audioUrl)
        return true
    }

/** Catat lagu ke riwayat (tanpa audioUrl). Dipanggil setelah lagu benar-benar mulai diputar. */
    internal fun RexPlayerController.__split_pushHistory_26(track: RexTrack) {
        val entry = track.copy(audioUrl = "")
        history.removeAll { sameSong(it, entry) }
        history.add(entry)
        while (history.size > MAX_HISTORY) history.removeAt(0)
        store.save(history)
        publishQueueInfo()
    }
