package com.rexaps.rexmusic

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import android.os.Environment
import java.net.Socket
import kotlin.math.max

internal fun RexMusicSyncEngine.__split_broadcastTick_15() {
        val now = SystemClock.elapsedRealtime()
        val roomElapsed = (now - roomStartedElapsedMs).coerceAtLeast(0L)
        _state.value = _state.value.copy(roomElapsedMs = roomElapsed)
        val roomMessage = JSONObject().put("type", "room_time").put("roomElapsedMs", roomElapsed)
        peers.filter { it.clockSynced }.forEach { runCatching { send(it.writer, roomMessage) } }
        if (hostPreparing) return
        val track = hostTrack ?: return
        val current = RexPlayerController.state.value
        // RexPlayerController has its own autoplay. If it advances away from the shared track,
        // hand control back to the room queue so every member gets the same next track.
        if (hostPlaying && SystemClock.elapsedRealtime() > playAnchorWallMs + 2500L &&
            current.isPlaying && current.nowPlaying?.id != null && current.nowPlaying?.id != track.id) {
            val next = sharedQueue.firstOrNull()
            if (next != null) { playQueued(next); return }
            stopAll("Lagu selesai · antrean bersama kosong")
            return
        }
        val position = if (hostPlaying) {
            if (current.nowPlaying?.id == track.id && current.positionMs > 0L) current.positionMs
            else playAnchorPositionMs + max(0L, SystemClock.elapsedRealtime() - playAnchorWallMs)
        } else current.positionMs
        val hostReference = if (hostPlaying) max(now, playAnchorWallMs) else now
        val msg = JSONObject().put("type", "sync").put("trackId", track.id).put("title", track.title)
            .put("position", position).put("playing", hostPlaying).put("hostElapsed", hostReference).put("roomElapsedMs", (now - roomStartedElapsedMs).coerceAtLeast(0L))
        peers.filter { it.clockSynced }.forEach { send(it.writer, msg) }
        _state.value = _state.value.copy(positionMs = position, playing = hostPlaying, roomElapsedMs = (SystemClock.elapsedRealtime() - roomStartedElapsedMs).coerceAtLeast(0L))
    }

internal fun RexMusicSyncEngine.__split_broadcastMembers_16() {
        val a = org.json.JSONArray().put(JSONObject().put("id", "host").put("name", "Host"))
        peers.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("volume", it.volume).put("deviceVolume", it.deviceVolume)) }
        _state.value = _state.value.copy(members = listOf(SyncMember("host", "Host")) + peers.map { SyncMember(it.id, it.name, ip = it.ip, volume = it.volume, deviceVolume = it.deviceVolume) }, volumes = peers.associate { it.id to it.volume })
        val msg = JSONObject().put("type", "members").put("members", a)
        peers.forEach { send(it.writer, msg) }
    }

fun RexMusicSyncEngine.__split_setHostVolume_19(value: Float) {
        val v = value.coerceIn(0f, 1f)
        _state.value = _state.value.copy(hostVolume = v)
        appContextRef?.let { context ->
            RexPlayerController.init(context)
            RexPlayerController.setSyncVolume(v)
        }
    }

fun RexMusicSyncEngine.__split_setMemberVolume_20(id: String, value: Float) {
        val v = value.coerceIn(0f, 1f)
        if (!_state.value.hosting) return
        peers.firstOrNull { it.id == id }?.let { p -> p.volume = v; runCatching { send(p.writer, JSONObject().put("type", "volume").put("value", v)) } }
        _state.value = _state.value.copy(volumes = _state.value.volumes + (id to v), members = _state.value.members.map { if (it.id == id) it.copy(volume = v) else it })
    }

internal suspend fun RexMusicSyncEngine.__split_sendBundle_25(host: String, track: RexTrack): Boolean = withContext(Dispatchers.IO) {
        try {
            val entry = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == track.id } ?: return@withContext false
            val root = RexOfflineManager.root
            val dir = File(root, entry.key)
            val files = listOf("music.mp3", "meta.json", "lyrics.json", "thumbnail.jpg", "thumbnail.png", "thumbnail.jpeg", "thumbnail.webp").map { File(dir, it) }.filter { it.isFile }
            if (files.none { it.name == "music.mp3" }) return@withContext false
            val temp = File.createTempFile("rexsync-", ".zip", appContextRef?.cacheDir)
            try {
                ZipOutputStream(temp.outputStream().buffered()).use { zip -> files.forEach { f -> zip.putNextEntry(ZipEntry(f.name)); f.inputStream().buffered().use { it.copyTo(zip) }; zip.closeEntry() } }
                Socket().use { socket -> socket.connect(java.net.InetSocketAddress(host, TRANSFER_PORT), 4000); socket.soTimeout = 30000
                    val out = DataOutputStream(socket.getOutputStream().buffered())
                    out.writeUTF(trackJson(track).toString()); out.writeLong(temp.length()); temp.inputStream().buffered().use { it.copyTo(out) }; out.flush()
                }
            } finally { temp.delete() }
            true
        } catch (_: Exception) { false }
    }

internal fun RexMusicSyncEngine.__split_receiveBundle_26(socket: Socket) {
        socket.use { s ->
            try {
                s.soTimeout = 20000
                val input = DataInputStream(s.getInputStream())
                val track = trackFromJson(JSONObject(input.readUTF())) ?: return
                val size = input.readLong(); if (size <= 0L || size > 600L * 1024L * 1024L) return
                val tempZip = File.createTempFile("rexsync-in-", ".zip", appContextRef?.cacheDir)
                try { tempZip.outputStream().buffered().use { out ->
                    var remaining = size; val buffer = ByteArray(64 * 1024)
                    while (remaining > 0L) { val n = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt()); if (n < 0) error("Transfer terputus"); out.write(buffer, 0, n); remaining -= n }
                }
                val base = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RexAps/.cache/rexmusicsync/${offlineKey(track)}")
                base.mkdirs()
                ZipInputStream(tempZip.inputStream().buffered()).use { zip -> var e = zip.nextEntry; while (e != null) {
                    val name = File(e.name).name
                    if (name in setOf("music.mp3", "meta.json", "lyrics.json", "thumbnail.jpg", "thumbnail.png", "thumbnail.jpeg", "thumbnail.webp")) File(base, name).outputStream().use { zip.copyTo(it) }
                    zip.closeEntry(); e = zip.nextEntry
                } }
                val audio = File(base, "music.mp3"); if (!audio.isFile || audio.length() == 0L) error("Cache audio kosong")
                File(base, "meta.json").writeText(trackJson(track).toString())
                val imported = RexOfflineManager.importSyncTrack(track, base)
                _state.value = _state.value.copy(cacheStatus = if (imported) "Cache siap: ${track.title}" else "Cache tersimpan; beri izin penyimpanan agar RexMusic bisa memutarnya", error = null)
                if (_state.value.hosting) {
                    if (sharedQueue.none { it.id == track.id }) sharedQueue += track
                    publishQueue()
                    val senderIp = s.inetAddress?.hostAddress
                    peers.filter { it.ip != senderIp }.forEach { peer -> scope.launch { delay(900L); sendBundle(peer.ip, track) } }
                }
                } finally { tempZip.delete() }
            } catch (e: Exception) { _state.value = _state.value.copy(cacheStatus = "Transfer gagal: ${e.message}") }
        }
    }

internal fun RexMusicSyncEngine.__split_send_27(w: BufferedWriter, m: JSONObject) {
        // Socket writes may fail when a phone disconnects mid-gesture. Never let an IOException
        // escape a Compose callback (especially the volume slider) and crash the whole screen.
        runCatching { synchronized(w) { w.write(m.toString()); w.newLine(); w.flush() } }
    }

internal fun RexMusicSyncEngine.__split_closeAll_28() {
        hostPreparing = false; hostPlaying = false; roomStartedElapsedMs = 0L; clockOffsetMs = 0L; lastClockRttMs = -1L
        heartbeat?.cancel(); playbackJob?.cancel(); roomJob?.cancel(); roomJob = null
        sharedQueue.clear()
        peers.forEach { runCatching { it.socket.close() } }; peers.clear()
        runCatching { client?.close() }; client = null; clientWriter = null
        runCatching { server?.close() }; server = null
        if (_state.value.hosting) stopLocalPlayback()
        _state.value = _state.value.copy(queue = emptyList())
    }
