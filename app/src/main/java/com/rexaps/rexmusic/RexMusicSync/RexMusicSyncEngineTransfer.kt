package com.rexaps.rexmusic

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

internal suspend fun RexMusicSyncEngine.distributeLocalTrack(track: RexTrack) {
    val entry = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == track.id } ?: return
    val peersSnapshot = peers.toList()
    for (p in peersSnapshot) {
        val ok = sendBundle(p.ip, track)
        if (!ok) _state.value = _state.value.copy(cacheStatus = "Gagal mengirim ke ${p.name}; cek Wi-Fi/firewall")
    }
    _state.value = _state.value.copy(cacheStatus = "Cache lagu siap untuk anggota yang terjangkau")
}

internal suspend fun RexMusicSyncEngine.sendBundle(host: String, track: RexTrack): Boolean = withContext(Dispatchers.IO) {
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

internal fun RexMusicSyncEngine.receiveBundle(socket: Socket) {
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

internal fun RexMusicSyncEngine.send(w: BufferedWriter, m: JSONObject) {
    // Socket writes may fail when a phone disconnects mid-gesture. Never let an IOException
    // escape a Compose callback (especially the volume slider) and crash the whole screen.
    runCatching { synchronized(w) { w.write(m.toString()); w.newLine(); w.flush() } }
}

internal suspend fun RexMusicSyncEngine.withContextMain(block: () -> Unit) { withContext(Dispatchers.Main) { block() } }
