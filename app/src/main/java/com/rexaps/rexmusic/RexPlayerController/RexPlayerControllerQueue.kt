package com.rexaps.rexmusic

import kotlinx.coroutines.flow.update

fun RexPlayerController.__split_createPlaylist_2(name: String) {
        if (playlistStore.create(name) == null) {
            postNotice("Nama playlist tidak valid atau sudah ada")
            return
        }
        refreshPlaylists()
        postNotice("Playlist dibuat: ${name.trim()}")
    }

fun RexPlayerController.__split_addToPlaylist_3(name: String, track: RexTrack) {
        if (playlistStore.addTrack(name, track)) {
            refreshPlaylists()
            postNotice("Ditambahkan ke $name")
        } else {
            postNotice("Playlist tidak bisa diperbarui")
        }
    }

fun RexPlayerController.__split_playPlaylist_4(name: String, startTrackId: String? = null) {
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

fun RexPlayerController.__split_addToQueue_9(track: RexTrack) {
        val entry = track.copy(audioUrl = "")
        when {
            userQueue.any { sameSong(it, entry) } -> postNotice("Sudah ada di antrian")
            userQueue.size >= MAX_QUEUE -> postNotice("Antrian penuh (maks $MAX_QUEUE lagu)")
            else -> {
                userQueue.add(entry)
                persistQueue()
                publishQueueInfo()
                postNotice("Ditambahkan ke antrian")
            }
        }
    }

fun RexPlayerController.__split_moveQueueToTop_10(index: Int) {
        if (index !in 1 until userQueue.size) return
        val track = userQueue.removeAt(index)
        userQueue.add(0, track)
        persistQueue()
        publishQueueInfo()
    }

/** Pastikan target ada di antrian konteks & queueIndex menunjuk ke sana. */
    internal fun RexPlayerController.__split_syncQueueTo_14(target: RexTrack) {
        val idx = queue.indexOfFirst { sameSong(it, target) }
        if (idx >= 0) {
            queueIndex = idx
            return
        }
        val at = queueIndex.coerceIn(0, queue.size)
        queue = queue.toMutableList().apply { add(at, target) }
        queueIndex = at
    }

internal fun RexPlayerController.__split_publishQueueInfo_28() {
        val now = _state.value.nowPlaying
        val hist = history.asReversed().filter { now == null || !sameSong(it, now) }
        val contextNext = !autoplay || followContext
        val up = userQueue.firstOrNull()
            ?: if (contextNext && queue.size > 1) queue[(queueIndex + 1) % queue.size] else null
        _state.update {
            it.copy(
                history = hist,
                upNext = up,
                previous = previousTarget(now),
                userQueue = userQueue.toList(),
                autoplay = autoplay
            )
        }
    }
