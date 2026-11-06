package com.rexaps.rexmusic

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import org.json.JSONObject

internal fun RexMusicSyncEngine.__splitImpl_handleClientMessage(m: JSONObject) {
        when (m.optString("type")) {
            "room_time" -> _state.value = _state.value.copy(roomElapsedMs = m.optLong("roomElapsedMs", _state.value.roomElapsedMs))
            "repeat" -> {
                val total = m.optInt("total", 0).coerceIn(0, MAX_SYNC_REPEAT)
                val left = m.optInt("left", total).coerceIn(0, MAX_SYNC_REPEAT)
                RexPlayerController.setSynchronizedRepeat(total, left)
                _state.value = _state.value.copy(repeatTotal = total, repeatLeft = left)
            }
            "pong" -> {
                val c0 = m.optLong("clientElapsed", 0L)
                val h1 = m.optLong("hostReceiveElapsed", 0L)
                val h2 = m.optLong("hostSendElapsed", 0L)
                val c3 = SystemClock.elapsedRealtime()
                if (c0 > 0L && h1 > 0L && h2 >= h1) {
                    val rtt = (c3 - c0 - (h2 - h1)).coerceAtLeast(0L)
                    val offset = (((h1 - c0) + (h2 - c3)) / 2L)
                    // Prefer low-RTT samples; they are less affected by queued network traffic.
                    if (lastClockRttMs < 0L || rtt <= lastClockRttMs + 40L || rtt < lastClockRttMs) {
                        clockOffsetMs = offset
                        lastClockRttMs = rtt
                    }
                }
            }
            "members" -> {
                val a = m.optJSONArray("members")
                val list = mutableListOf<SyncMember>()
                if (a != null) for (i in 0 until a.length()) {
                    val o = a.optJSONObject(i) ?: continue
                    list += SyncMember(
                        id = o.optString("id"), name = o.optString("name"),
                        volume = o.optDouble("volume", 1.0).toFloat().coerceIn(0f, 1f),
                        deviceVolume = o.optDouble("deviceVolume", -1.0).toFloat().coerceIn(-1f, 1f),
                        username = o.optString("username", ""), bio = o.optString("bio", ""),
                        profilePictureBase64 = o.optString("profilePicture", "").take(260_000),
                        bannerBase64 = o.optString("banner", "").take(420_000),
                        profilePictureVideoBase64 = o.optString("profilePictureVideo", "").take(8_400_000),
                        bannerVideoBase64 = o.optString("bannerVideo", "").take(8_400_000)
                    )
                }
                _state.value = _state.value.copy(members = list)
            }
            "play_prepare" -> {
                val id = m.optString("trackId")
                val position = m.optLong("position", 0L).coerceAtLeast(0L)
                val startAt = m.optLong("startAt", 0L)
                val title = m.optString("title", "Belum ada lagu")
                val total = m.optInt("repeatTotal", _state.value.repeatTotal).coerceIn(0, MAX_SYNC_REPEAT)
                val left = m.optInt("repeatLeft", total).coerceIn(0, MAX_SYNC_REPEAT)
                RexPlayerController.setSynchronizedRepeat(total, left)
                if (id.isNotBlank() && startAt > 0L) {
                    playbackJob?.cancel()
                    _state.value = _state.value.copy(trackId = id, trackTitle = title,
                        positionMs = position, playing = true,
                        roomElapsedMs = m.optLong("roomElapsedMs", _state.value.roomElapsedMs),
                        resyncing = true, status = "Menyiapkan waktu mulai bersama…", error = null,
                        repeatTotal = total, repeatLeft = left)
                    playbackJob = scope.launch { syncLocalTrack(id, position, true, startAt) }
                }
            }
            "room_play" -> {
                val id = m.optString("trackId")
                val position = m.optLong("position", 0L).coerceAtLeast(0L)
                val startAt = m.optLong("startAt", 0L)
                val title = m.optString("title", _state.value.trackTitle)
                if (id.isNotBlank() && startAt > 0L) {
                    playbackJob?.cancel()
                    _state.value = _state.value.copy(trackId = id, trackTitle = title,
                        positionMs = position, playing = true,
                        roomElapsedMs = m.optLong("roomElapsedMs", _state.value.roomElapsedMs),
                        resyncing = true, status = "Resume serentak dijadwalkan…", error = null)
                    playbackJob = scope.launch {
                        val local = RexPlayerController.state.value
                        if (local.nowPlaying?.id == id && local.durationMs > 0L) {
                            val targetAtStart = (position + (SystemClock.elapsedRealtime() + clockOffsetMs - startAt).coerceAtLeast(0L))
                                .coerceAtMost(local.durationMs)
                            withContext(Dispatchers.Main) {
                                RexPlayerController.pause()
                                RexPlayerController.prepareSyncSeek(targetAtStart)
                            }
                            val deadline = startAt - clockOffsetMs
                            delay((deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
                            withContext(Dispatchers.Main) { RexPlayerController.startSyncPlayback(targetAtStart) }
                        } else {
                            syncLocalTrack(id, position, true, startAt)
                        }
                        _state.value = _state.value.copy(playing = true, resyncing = false, status = "Diputar bersama")
                    }
                }
            }
            "room_pause" -> {
                val id = m.optString("trackId")
                val position = m.optLong("position", 0L).coerceAtLeast(0L)
                if (id.isNotBlank() && RexPlayerController.state.value.nowPlaying?.id == id) {
                    playbackJob?.cancel()
                    scope.launch {
                        withContext(Dispatchers.Main) {
                            RexPlayerController.pause()
                            RexPlayerController.seekToPositionImmediate(position)
                        }
                    }
                }
                _state.value = _state.value.copy(trackId = id, positionMs = position, playing = false,
                    roomElapsedMs = m.optLong("roomElapsedMs", _state.value.roomElapsedMs),
                    resyncing = false, status = "Dijeda host")
            }
            "room_snapshot" -> {
                val id = m.optString("trackId")
                val position = m.optLong("position", 0L).coerceAtLeast(0L)
                val playing = m.optBoolean("playing", false)
                val title = m.optString("title", "Belum ada lagu")
                val total = m.optInt("repeatTotal", _state.value.repeatTotal).coerceIn(0, MAX_SYNC_REPEAT)
                val left = m.optInt("repeatLeft", total).coerceIn(0, MAX_SYNC_REPEAT)
                RexPlayerController.setSynchronizedRepeat(total, left)
                _state.value = _state.value.copy(trackId = id, trackTitle = title,
                    positionMs = position, playing = playing,
                    roomElapsedMs = m.optLong("roomElapsedMs", _state.value.roomElapsedMs),
                    repeatTotal = total, repeatLeft = left)
                if (id.isNotBlank() && RexPlayerController.state.value.nowPlaying?.id != id && playbackJob?.isActive != true) {
                    playbackJob = scope.launch { syncLocalTrack(id, position, false, 0L) }
                } else if (!playing && id.isNotBlank() && RexPlayerController.state.value.nowPlaying?.id == id && RexPlayerController.state.value.isPlaying) {
                    scope.launch { withContext(Dispatchers.Main) { RexPlayerController.pause(); RexPlayerController.seekToPositionImmediate(position) } }
                }
            }
            "room_sync" -> {
                val id = m.optString("trackId")
                val title = m.optString("title", _state.value.trackTitle)
                val pos = m.optLong("position", _state.value.positionMs).coerceAtLeast(0L)
                val playing = m.optBoolean("playing", _state.value.playing)
                val hostAt = m.optLong("hostElapsed", 0L)
                val projected = if (playing && hostAt > 0L) pos + (SystemClock.elapsedRealtime() + clockOffsetMs - hostAt).coerceAtLeast(0L) else pos
                val total = m.optInt("repeatTotal", _state.value.repeatTotal).coerceIn(0, MAX_SYNC_REPEAT)
                val left = m.optInt("repeatLeft", total).coerceIn(0, MAX_SYNC_REPEAT)
                RexPlayerController.setSynchronizedRepeat(total, left)
                // Important: passive telemetry never invokes pause/play/seek. The host
                // sends an explicit room_resync only when reported drift exceeds 1 second.
                _state.value = _state.value.copy(trackId = id, trackTitle = title,
                    positionMs = projected, playing = playing,
                    roomElapsedMs = m.optLong("roomElapsedMs", _state.value.roomElapsedMs),
                    repeatTotal = total, repeatLeft = left)
            }
            "stop" -> { playbackJob?.cancel(); stopLocalPlayback(); _state.value = _state.value.copy(playing = false, status = m.optString("reason", "Pemutaran dihentikan")) }
            "queue" -> {
                val arr = m.optJSONArray("items"); val list = mutableListOf<RexTrack>()
                if (arr != null) for (i in 0 until arr.length()) trackFromJson(arr.optJSONObject(i))?.let { list += it }
                _state.value = _state.value.copy(queue = list)
            }
            "queue_item" -> trackFromJson(m.optJSONObject("track"))?.let { t -> if (_state.value.queue.none { it.id == t.id }) _state.value = _state.value.copy(queue = _state.value.queue + t) }
            "room_resync" -> {
                val id = m.optString("trackId")
                val position = m.optLong("position", 0L).coerceAtLeast(0L)
                val startAt = m.optLong("startAt", 0L)
                val roomElapsed = m.optLong("roomElapsedMs", _state.value.roomElapsedMs).coerceAtLeast(0L)
                if (id.isNotBlank() && startAt > 0L) {
                    playbackJob?.cancel()
                    _state.value = _state.value.copy(
                        playing = false, positionMs = position, roomElapsedMs = roomElapsed,
                        resyncing = true, lastResyncAtMs = SystemClock.elapsedRealtime(),
                        status = if (m.optString("reason") == "automatic_drift") "Room sedang rewind 1 detik…" else "Menunggu refresh serentak…", error = null
                    )
                    playbackJob = scope.launch { applyRemoteRefresh(id, position, startAt, roomElapsed) }
                }
            }
            "volume" -> {
                val v = m.optDouble("value", 1.0).toFloat().coerceIn(0f, 1f)
                scope.launch(Dispatchers.Main) {
                    val context = appContextRef
                    val actual = context?.let { setAndroidMusicVolume(it, v) } ?: -1f
                    // Do not multiply system volume by a second in-app gain factor.
                    RexPlayerController.setSyncVolume(1f)
                    if (actual < 0f) {
                        _state.value = _state.value.copy(error = "Volume Android tidak dapat diubah pada perangkat ini.")
                    } else {
                        _state.value = _state.value.copy(error = null)
                    }
                }
            }
            "reject" -> _state.value = _state.value.copy(error = m.optString("reason"), status = "Akses ditolak")
        }
    }
