package com.rexaps.rexmusic

import kotlinx.coroutines.flow.update

internal fun RexPlayerController.__splitExtra_refreshPlaylists_4() {
        if (!initialized) return
        _state.update { it.copy(playlists = playlistStore.list()) }
    }

internal fun RexPlayerController.__splitExtra_removeFromPlaylist_5(name: String, track: RexTrack) {
        if (playlistStore.removeTrack(name, track.id)) {
            refreshPlaylists()
            postNotice("Dihapus dari $name")
        }
    }

internal fun RexPlayerController.__splitExtra_deletePlaylist_6(name: String) {
        if (playlistStore.delete(name)) {
            refreshPlaylists()
            postNotice("Playlist dihapus: $name")
        }
    }

internal fun RexPlayerController.__splitExtra_startQueue_15(list: List<RexTrack>, index: Int, follow: Boolean = false) {
        cancelPending()
        queue = list
        queueIndex = index
        followContext = follow
        beginTrack(list[index])
    }

internal fun RexPlayerController.__splitExtra_removeFromQueue_17(index: Int) {
        if (index !in userQueue.indices) return
        userQueue.removeAt(index)
        persistQueue()
        publishQueueInfo()
    }

internal fun RexPlayerController.__splitExtra_clearQueue_18() {
        if (userQueue.isEmpty()) return
        userQueue.clear()
        persistQueue()
        publishQueueInfo()
        postNotice("Antrian dikosongkan")
    }

/** Putar lagu dari antrian sekarang juga (lagunya keluar dari antrian). */
    internal fun RexPlayerController.__splitExtra_playFromQueue_19(index: Int) {
        if (index !in userQueue.indices) return
        val track = userQueue.removeAt(index)
        persistQueue()
        cancelPending()
        beginTrack(track)
    }

internal fun RexPlayerController.__splitExtra_persistQueue_20() = queueStore.save(userQueue.toList())
