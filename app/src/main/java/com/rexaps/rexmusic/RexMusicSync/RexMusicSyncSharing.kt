package com.rexaps.rexmusic

import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Repeat is authoritative on the host and mirrored to every room member. */
internal fun RexMusicSyncEngine.__splitImpl_setRepeat(times: Int) {
    val count = times.coerceIn(0, MAX_SYNC_REPEAT)
    if (_state.value.hosting) {
        RexPlayerController.setSynchronizedRepeat(count, count)
        _state.value = _state.value.copy(repeatTotal = count, repeatLeft = count, error = null)
        val update = JSONObject().put("type", "repeat").put("total", count).put("left", count)
        peers.toList().forEach { send(it.writer, update) }
    } else if (_state.value.connected) {
        runCatching {
            clientWriter?.let { send(it, JSONObject().put("type", "repeat_request").put("total", count)) }
        }.onFailure {
            _state.value = _state.value.copy(error = "Tidak dapat mengirim pengaturan repeat ke host.")
        }
        _state.value = _state.value.copy(status = "Meminta host mengubah repeat menjadi $count×")
    }
}

/**
 * Shares every track in a user's playlist. Missing tracks are downloaded first on
 * the sender (the offline manager still limits downloads to two concurrent jobs),
 * then the sender uploads each audio bundle to the host. The host fans it out to
 * every room member through the existing transfer socket.
 */
internal fun RexMusicSyncEngine.__splitImpl_addPlaylistToQueue(playlist: RexPlaylist) {
    if (!_state.value.hosting && !_state.value.connected) return
    val allTracks = playlist.tracks.distinctBy { it.id }.filter { it.id.isNotBlank() }
    if (allTracks.isEmpty()) {
        _state.value = _state.value.copy(error = "Playlist '${playlist.name}' belum memiliki lagu.")
        return
    }

    scope.launch {
        _state.value = _state.value.copy(error = null, cacheStatus = "Menyiapkan playlist ${playlist.name}…")
        val initiallyMissing = allTracks.filterNot { track ->
            RexOfflineManager.state.value.entries.any { it.track.id == track.id }
        }
        if (initiallyMissing.isNotEmpty()) {
            if (!RexOfflineManager.state.value.hasAccess) {
                _state.value = _state.value.copy(
                    error = "Izin penyimpanan diperlukan untuk mengunduh seluruh lagu playlist sebelum dibagikan.",
                    cacheStatus = "Playlist belum dibagikan"
                )
                return@launch
            }
            initiallyMissing.forEach { track -> RexOfflineManager.download(track) }

            val downloadDeadline = SystemClock.elapsedRealtime() + 10L * 60L * 1000L
            while (SystemClock.elapsedRealtime() < downloadDeadline) {
                val readyIds = RexOfflineManager.state.value.entries.map { it.track.id }.toSet()
                val remaining = initiallyMissing.filterNot { it.id in readyIds }
                if (remaining.isEmpty()) break
                val downloading = remaining.any { track -> RexOfflineManager.jobs[offlineKey(track)]?.isActive == true }
                _state.value = _state.value.copy(
                    cacheStatus = "Mengunduh lagu playlist: ${allTracks.size - remaining.size}/${allTracks.size} siap"
                )
                // All jobs have finished but some files did not arrive: stop waiting and report them.
                if (!downloading) break
                delay(600L)
            }
        }

        val offlineById = RexOfflineManager.state.value.entries.associateBy { it.track.id }
        val available = allTracks.mapNotNull { track -> offlineById[track.id]?.track }
        val missing = allTracks.size - available.size
        if (available.isEmpty()) {
            _state.value = _state.value.copy(
                error = "Tidak ada lagu playlist '${playlist.name}' yang berhasil disiapkan. Periksa koneksi dan izin penyimpanan.",
                cacheStatus = "0/${allTracks.size} lagu siap dibagikan"
            )
            return@launch
        }

        var sent = 0
        var failures = 0
        for (track in available) {
            if (_state.value.hosting) {
                if (sharedQueue.none { it.id == track.id }) {
                    sharedQueue += track
                    publishQueue()
                }
                // Retry transfer even for existing queue entries, allowing failed members to recover.
                peers.toList().forEach { peer ->
                    if (!sendBundle(peer.ip, track)) failures++
                }
            } else {
                val host = _state.value.ip
                val writer = clientWriter
                if (host.isBlank() || writer == null) {
                    failures++
                    continue
                }
                send(writer, JSONObject().put("type", "queue_add").put("track", trackJson(track)))
                if (!sendBundle(host, track)) failures++
            }
            sent++
            _state.value = _state.value.copy(
                cacheStatus = "Membagikan playlist ${playlist.name}: $sent/${available.size} lagu"
            )
        }

        val suffix = buildList {
            if (missing > 0) add("$missing gagal diunduh")
            if (failures > 0) add("$failures transfer gagal")
        }.joinToString(" · ")
        _state.value = _state.value.copy(
            cacheStatus = if (suffix.isBlank()) "Playlist ${playlist.name} siap di room · $sent lagu" else "Playlist ${playlist.name}: $sent/${available.size} dikirim · $suffix",
            error = when {
                failures > 0 -> "Sebagian lagu gagal ditransfer. Periksa Wi-Fi dan ruang penyimpanan perangkat anggota, lalu bagikan ulang playlist untuk mencoba lagi."
                missing > 0 -> "Sebagian lagu gagal diunduh di perangkat pengirim, sehingga belum dapat dibagikan."
                else -> null
            }
        )
    }
}
