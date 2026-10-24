package com.rexaps.rexmusic

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import org.json.JSONObject
import kotlin.math.max

fun RexMusicSyncEngine.__split_playTrack_10(track: RexTrack) {
        if (!_state.value.hosting) return
        val local = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == track.id }
        if (local == null) { _state.value = _state.value.copy(error = "Pilih lagu yang sudah tersedia offline di HP host."); return }
        hostTrack = local.track
        playAnchorPositionMs = 0L
        hostPreparing = true
        hostPlaying = false
        _state.value = _state.value.copy(trackId = track.id, trackTitle = track.title, positionMs = 0L, playing = false, status = "Menyiapkan semua perangkat…", error = null)
        playbackJob?.cancel()
        playbackJob = scope.launch {
            // Prepare the host player before announcing the future start; do not play early.
            withContext(Dispatchers.Main) { RexPlayerController.prepareFromOfflineForSync(local.track) }
            val deadline = SystemClock.elapsedRealtime() + 15000L
            while (isActive && SystemClock.elapsedRealtime() < deadline) {
                val s = RexPlayerController.state.value
                if (s.nowPlaying?.id == local.track.id && s.durationMs > 0L && s.phase == LoadPhase.Idle) break
                delay(40L)
            }
            val ready = RexPlayerController.state.value
            if (ready.nowPlaying?.id != local.track.id || ready.durationMs <= 0L) {
                hostPreparing = false
                hostPlaying = false
                _state.value = _state.value.copy(playing = false, status = "Lagu gagal disiapkan", error = "Player tidak siap sebelum batas waktu.")
                broadcastState()
                return@launch
            }
            hostPreparing = false
            hostPlaying = true
            playAnchorWallMs = SystemClock.elapsedRealtime() + START_LEAD_MS
            _state.value = _state.value.copy(playing = true, status = "Mulai serentak · room clock")
            broadcastState()
            delay(max(0L, playAnchorWallMs - SystemClock.elapsedRealtime()))
            withContext(Dispatchers.Main) { RexPlayerController.startSyncPlayback(0L) }
        }
    }

internal fun RexMusicSyncEngine.__split_resumeRoomAtCurrentPosition_11() {
        val track = hostTrack ?: return
        playAnchorPositionMs = RexPlayerController.state.value.positionMs.coerceAtLeast(playAnchorPositionMs)
        playAnchorWallMs = SystemClock.elapsedRealtime() + START_LEAD_MS
        hostPlaying = true
        _state.value = _state.value.copy(playing = true, positionMs = playAnchorPositionMs, status = "Anggota kembali · sinkronisasi ulang", error = null)
        broadcastState()
        playbackJob?.cancel()
        playbackJob = scope.launch {
            delay(max(0L, playAnchorWallMs - SystemClock.elapsedRealtime()))
            withContext(Dispatchers.Main) {
                if (RexPlayerController.state.value.nowPlaying?.id != track.id) RexPlayerController.playFromOffline(track)
                delay(150L)
                RexPlayerController.seekToPositionImmediate(playAnchorPositionMs)
                RexPlayerController.startSyncPlayback(playAnchorPositionMs)
            }
        }
    }

fun RexMusicSyncEngine.__split_togglePlayback_12() {
        if (!_state.value.hosting) return
        hostPlaying = !hostPlaying
        if (hostPlaying) {
            playAnchorPositionMs = RexPlayerController.state.value.positionMs
            playAnchorWallMs = SystemClock.elapsedRealtime() + START_LEAD_MS
            broadcastState()
            playbackJob?.cancel(); playbackJob = scope.launch { delay(max(0L, playAnchorWallMs-SystemClock.elapsedRealtime())); withContext(Dispatchers.Main) { RexPlayerController.seekToPositionImmediate(playAnchorPositionMs); RexPlayerController.startSyncPlayback(playAnchorPositionMs) } }
        } else {
            scope.launch { withContextMain { RexPlayerController.pause() } }
            _state.value = _state.value.copy(playing = false, positionMs = RexPlayerController.state.value.positionMs, status = "Dijeda host")
            broadcastState()
        }
    }

internal fun RexMusicSyncEngine.__split_trackFromJson_22(o: JSONObject?): RexTrack? = o?.let { RexTrack(it.optString("id"), it.optString("title"), it.optString("artist"), it.optString("album", "Single"), it.optString("cover"), spotifyUrl = it.optString("spotifyUrl"), durationText = it.optString("durationText")) }?.takeIf { it.id.isNotBlank() && it.title.isNotBlank() }

internal suspend fun RexMusicSyncEngine.__split_distributeLocalTrack_24(track: RexTrack) {
        val entry = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == track.id } ?: return
        val peersSnapshot = peers.toList()
        for (p in peersSnapshot) {
            val ok = sendBundle(p.ip, track)
            if (!ok) _state.value = _state.value.copy(cacheStatus = "Gagal mengirim ke ${p.name}; cek Wi-Fi/firewall")
        }
        _state.value = _state.value.copy(cacheStatus = "Cache lagu siap untuk anggota yang terjangkau")
    }
