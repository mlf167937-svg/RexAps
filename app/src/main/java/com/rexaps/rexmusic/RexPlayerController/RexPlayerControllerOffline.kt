package com.rexaps.rexmusic

import kotlin.random.Random
import kotlinx.coroutines.flow.update

internal fun RexPlayerController.offlineMode(): Boolean = RexOfflineManager.state.value.enabled

internal fun RexPlayerController.onOfflineChanged(o: OfflineState) {
    val modeChanged = _state.value.offline.enabled != o.enabled
    _state.update { it.copy(offline = o) }
    if (modeChanged) publishQueueInfo()
}

//  Offline mode 

internal fun RexPlayerController.setOfflineModeImpl(on: Boolean) {
    RexOfflineManager.setOfflineMode(on)
    clearSearch()
    postNotice(if (on) "Mode offline aktif. Hanya lagu unduhan yang diputar" else "Kembali online")
}

/** Putar lagu dari perpustakaan offline; Next mengikuti urutan perpustakaan. */
internal fun RexPlayerController.playFromOfflineImpl(track: RexTrack) {
    val list = RexOfflineManager.state.value.entries.map { it.track }
    val index = list.indexOfFirst { it.id == track.id }
    if (index < 0) return
    startQueue(list, index, follow = true)
}

/** Prepare a local track for synchronized playback without emitting audio before the room target time. */
internal fun RexPlayerController.prepareFromOfflineForSyncImpl(track: RexTrack) {
    val list = RexOfflineManager.state.value.entries.map { it.track }
    val index = list.indexOfFirst { it.id == track.id }
    if (index < 0) return
    syncPrepareTrackId = track.id
    startQueue(list, index, follow = true)
}

/** "Putar semua" / "Acak" di perpustakaan offline. */
internal fun RexPlayerController.playOfflineAllImpl(shuffle: Boolean) {
    val list = RexOfflineManager.state.value.entries.map { it.track }
    if (list.isEmpty()) {
        postNotice("Belum ada lagu offline")
        return
    }
    startQueue(if (shuffle) list.shuffled() else list, 0, follow = true)
}

internal fun RexPlayerController.offlinePick(avoid: Set<String>): RexTrack? {
    val all = RexOfflineManager.state.value.entries.map { it.track }
    if (all.isEmpty()) return null
    val favorite = taste.pickFavorite()
    if (favorite != null && Random.nextFloat() < TASTE_CHANCE) {
        all.filter {
            primaryArtist(it.artist).equals(favorite, ignoreCase = true) && songKey(it) !in avoid
        }.randomOrNull()?.let { return it }
    }
    val nowKey = _state.value.nowPlaying?.let { songKey(it) }
    return all.filter { songKey(it) !in avoid }.randomOrNull()
        ?: all.filter { songKey(it) != nowKey }.randomOrNull()
        ?: all.randomOrNull()
}
