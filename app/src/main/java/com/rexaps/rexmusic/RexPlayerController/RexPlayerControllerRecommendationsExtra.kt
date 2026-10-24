package com.rexaps.rexmusic

import kotlinx.coroutines.flow.update

internal fun RexPlayerController.__splitExtra_clearSearch_7() {
        searchJob?.cancel()
        searching = false
        _state.update { it.copy(searchResults = emptyList()) }
        publishLoading()
    }

internal fun RexPlayerController.__splitExtra_clearHistory_11() {
        history.clear()
        store.save(history)
        publishQueueInfo()
    }

internal fun RexPlayerController.__splitExtra_playFromSearch_13(index: Int) {
        val list = _state.value.searchResults
        if (index !in list.indices) return
        startQueue(list, index)
    }

/** index mengacu ke state.history (terbaru di depan). Lagu yang sudah diunduh diputar dari file. */
    internal fun RexPlayerController.__splitExtra_playFromHistory_14(index: Int) {
        val list = _state.value.history
        if (index !in list.indices) return
        startQueue(list, index)
    }

/** Mix pintar: langsung pilih lagu dari artis favorit / populer (atau dari unduhan saat offline). */
    internal fun RexPlayerController.__splitExtra_playMix_16() {
        cancelPending()
        followContext = false
        playSmart()
    }

internal fun RexPlayerController.__splitExtra_setAutoplay_22(on: Boolean) {
        autoplay = on
        taste.autoplay = on
        publishQueueInfo()
        postNotice(if (on) "Autoplay pintar aktif" else "Mengikuti urutan daftar")
    }

internal fun RexPlayerController.__splitExtra_publishTaste_30() {
        _state.update { it.copy(favoriteArtists = taste.favorites(3)) }
    }
