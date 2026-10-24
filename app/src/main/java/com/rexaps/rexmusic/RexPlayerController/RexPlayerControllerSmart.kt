package com.rexaps.rexmusic

import kotlin.random.Random
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//  Smart autoplay 

/** Pilih lagu berikutnya sendiri; coba sampai [SMART_RETRIES] kali kalau audio gagal dimuat. */
internal fun RexPlayerController.playSmart() {
    stopPlayer()
    resolving = true
    resolveText = "memilih lagu untukmu..."
    _state.update {
        it.copy(
            isPlaying = false, positionMs = 0L, durationMs = 0L, bufferedPercent = 0,
            seekVersion = it.seekVersion + 1, error = null, repeatTotal = 0, repeatLeft = 0,
            nowPlayingOffline = false
        )
    }
    publishLoading()
    RexMusicService.start(appContext)

    val job = scope.launch(start = CoroutineStart.LAZY) {
        val tried = mutableSetOf<String>()
        var attempts = 0
        while (attempts < SMART_RETRIES) {
            val pick = smartPick(tried) ?: break
            tried += songKey(pick)
            markTrack(pick)
            if (resolveAndStart(pick)) return@launch
            attempts++
        }
        if (attempts == 0) {
            resolving = false
            publishLoading()
            when {
                offlineMode() && RexOfflineManager.state.value.entries.isEmpty() ->
                    _state.update { it.copy(error = "belum ada lagu offline. Unduh lagu dulu saat online") }
                queue.isNotEmpty() -> playContextNext()
                else -> _state.update {
                    it.copy(error = "belum ada lagu berikutnya. Isi RexPopularSongs atau putar dari daftar")
                }
            }
        }
    }
    playJob = job
    job.start()
}

/**
 * Online: ~65% dari artis favorit (kalau selera sudah terbaca), selain itu acak dari populer.
 * Offline: sama, tapi hanya dari lagu yang sudah diunduh (tanpa internet).
 * Lagu yang baru diputar dihindari.
 */
private suspend fun RexPlayerController.smartPick(exclude: Set<String>): RexTrack? {
    val avoid = recentKeys() + exclude
    if (offlineMode()) return offlinePick(avoid)

    val favorite = taste.pickFavorite()
    if (favorite != null && Random.nextFloat() < TASTE_CHANCE) {
        pickFromArtist(favorite, avoid)?.let { return it }
    }
    popularPick(avoid)?.let { return it }
    if (favorite != null) pickFromArtist(favorite, avoid)?.let { return it }
    return null
}

private suspend fun RexPlayerController.pickFromArtist(artist: String, avoid: Set<String>): RexTrack? {
    val list = RexMusicApi.search(artist).getOrNull() ?: return null
    val key = artist.lowercase()
    return list
        .filter { it.artist.lowercase().contains(key) && songKey(it) !in avoid }
        .randomOrNull()
}

private fun RexPlayerController.popularPick(avoid: Set<String>): RexTrack? {
    val all = RexPopularSongs.tracks
    if (all.isEmpty()) return null
    val nowKey = _state.value.nowPlaying?.let { songKey(it) }
    return all.filter { songKey(it) !in avoid }.randomOrNull()
        ?: all.filter { songKey(it) != nowKey }.randomOrNull()
}

private fun RexPlayerController.recentKeys(): Set<String> {
    val keys = history.takeLast(RECENT_WINDOW).map { songKey(it) }.toMutableSet()
    _state.value.nowPlaying?.let { keys += songKey(it) }
    return keys
}

internal fun RexPlayerController.penalizeEarlySkip() {
    val now = _state.value.nowPlaying ?: return
    if (prepared && !tasteCounted && _state.value.positionMs < EARLY_SKIP_MS) {
        taste.recordSkip(now.artist)
        publishTaste()
    }
}

internal fun RexPlayerController.publishTaste() {
    _state.update { it.copy(favoriteArtists = taste.favorites(3)) }
}

//  History 

internal fun RexPlayerController.sameSong(a: RexTrack, b: RexTrack): Boolean =
    a.id == b.id ||
        (a.title.equals(b.title, ignoreCase = true) && a.artist.equals(b.artist, ignoreCase = true))
