package com.rexaps.rexmusic

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import android.os.Environment
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

private const val SYNC_PORT = 38741
private const val TRANSFER_PORT = 38742
private const val HEARTBEAT_MS = 800L
private const val CLIENT_TIMEOUT_MS = 3500L
private const val START_LEAD_MS = 1800L

private data class SyncMember(val id: String, val name: String, val connectedAt: Long = System.currentTimeMillis(), val ip: String = "", val volume: Float = 1f)
private data class SyncUiState(
    val hosting: Boolean = false, val connected: Boolean = false, val room: String = "RexSyncRoom1",
    val ip: String = "", val password: String = "rexsync", val name: String = "User",
    val status: String = "Belum terhubung", val members: List<SyncMember> = emptyList(),
    val trackId: String = "", val trackTitle: String = "Belum ada lagu", val positionMs: Long = 0L,
    val playing: Boolean = false, val error: String? = null, val queue: List<RexTrack> = emptyList(), val cacheStatus: String = "", val volumes: Map<String, Float> = emptyMap()
)

/** Local Wi-Fi sync protocol. Uses newline-delimited JSON control messages; no SSH or internet. */
private object RexMusicSyncEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private val _state = MutableStateFlow(SyncUiState())
    val state = _state.asStateFlow()
    private var server: ServerSocket? = null
    private var transferServer: ServerSocket? = null
    private var transferJob: Job? = null
    private val sharedQueue = mutableListOf<RexTrack>()
    private var roomJob: Job? = null
    private val peers = CopyOnWriteArrayList<Peer>()
    private var client: Socket? = null
    private var clientWriter: BufferedWriter? = null
    private var heartbeat: Job? = null
    private var playbackJob: Job? = null
    private var roomPassword = "rexsync"
    private var roomName = "RexSyncRoom1"
    private var hostTrack: RexTrack? = null
    private var playAnchorWallMs = 0L
    private var playAnchorPositionMs = 0L
    private var hostPlaying = false
    private var resumeWhenMemberReturns = false

    private data class Peer(val socket: Socket, val reader: BufferedReader, val writer: BufferedWriter,
                            var id: String = "", var name: String = "", @Volatile var lastSeen: Long = System.currentTimeMillis(), val ip: String = socket.inetAddress?.hostAddress.orEmpty(), @Volatile var volume: Float = 1f)

    fun detectIp(context: Context): String = runCatching {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wifi.connectionInfo.ipAddress
        if (ip == 0) "" else Formatter.formatIpAddress(ip)
    }.getOrDefault("")

    fun createRoom(room: String, password: String, ip: String) {
        closeAll()
        roomName = room.trim().ifBlank { "RexSyncRoom1" }
        roomPassword = password
        _state.value = _state.value.copy(hosting = true, connected = true, room = roomName,
            password = password, ip = ip, status = "Membuka room…", error = null,
            members = listOf(SyncMember("host", "Host")))
        roomJob = scope.launch {
            try {
                val ss = ServerSocket(SYNC_PORT, 32, InetAddress.getByName("0.0.0.0"))
                server = ss
                _state.value = _state.value.copy(status = "Room aktif · menunggu anggota")
                launch { monitorPeers() }
                launch { while (isActive && _state.value.hosting) { broadcastTick(); delay(900L) } }
                while (!ss.isClosed) {
                    val socket = ss.accept().apply { soTimeout = 0; tcpNoDelay = true; keepAlive = true }
                    launch { acceptPeer(socket) }
                }
            } catch (e: Exception) {
                if (_state.value.hosting) _state.value = _state.value.copy(status = "Server gagal: ${e.message}", error = e.message)
            }
        }
    }

    private suspend fun acceptPeer(socket: Socket) {
        val p = runCatching { Peer(socket, BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8)),
            BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))) }.getOrNull() ?: return
        try {
            val first = withContext(Dispatchers.IO) { p.reader.readLine() } ?: return
            val join = JSONObject(first)
            if (join.optString("type") != "join" || join.optString("room") != roomName || join.optString("password") != roomPassword) {
                send(p.writer, JSONObject().put("type", "reject").put("reason", "Nama room atau password salah")); return
            }
            p.id = join.optString("id").ifBlank { java.util.UUID.randomUUID().toString() }
            p.name = join.optString("name").trim().ifBlank { "User" }
            p.lastSeen = System.currentTimeMillis()
            peers.add(p)
            send(p.writer, JSONObject().put("type", "welcome").put("id", p.id).put("room", roomName))
            broadcastMembers()
            sendState(p); sendQueue(p); broadcastMembers()
            while (true) {
                val line = withContext(Dispatchers.IO) { p.reader.readLine() } ?: break
                val msg = JSONObject(line)
                when (msg.optString("type")) {
                    "ping" -> { p.lastSeen = System.currentTimeMillis(); send(p.writer, JSONObject().put("type", "pong").put("at", System.currentTimeMillis())) }
                    "ready" -> { p.lastSeen = System.currentTimeMillis() }
                    "request_track" -> { p.lastSeen = System.currentTimeMillis(); if (hostTrack != null) sendState(p) }
                    "queue_add" -> {
                        p.lastSeen = System.currentTimeMillis()
                        val t = trackFromJson(msg.optJSONObject("track"))
                        if (t != null && sharedQueue.none { it.id == t.id }) { sharedQueue += t; publishQueue() }
                        _state.value = _state.value.copy(cacheStatus = "Menerima ${t?.title ?: "lagu"} dari ${p.name}…")
                    }
                    "queue_request" -> { p.lastSeen = System.currentTimeMillis(); sendQueue(p) }
                    "volume" -> { p.lastSeen = System.currentTimeMillis(); p.volume = msg.optDouble("value", 1.0).toFloat().coerceIn(0f, 1f) }
                    "play_request" -> { p.lastSeen = System.currentTimeMillis(); val id = msg.optString("trackId"); sharedQueue.firstOrNull { it.id == id }?.let { queued -> val local = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == id }; if (local != null) { sharedQueue.removeAll { it.id == id }; publishQueue(); playTrack(local.track) } } }
                }
            }
        } catch (_: Exception) {
        } finally {
            peers.remove(p); runCatching { p.socket.close() }
            if (p.id.isNotBlank() && _state.value.hosting) {
                // Keep playback running for remaining members. A returning member receives the latest state.
                playAnchorPositionMs = RexPlayerController.state.value.positionMs.coerceAtLeast(playAnchorPositionMs)
                resumeWhenMemberReturns = hostTrack != null && hostPlaying
                broadcastMembers()
            }
        }
    }

    private suspend fun monitorPeers() {
        while (_state.value.hosting) {
            val now = System.currentTimeMillis()
            peers.filter { now - it.lastSeen > CLIENT_TIMEOUT_MS }.forEach { p -> runCatching { p.socket.close() } }
            delay(HEARTBEAT_MS)
        }
    }

    fun joinRoom(room: String, name: String, ip: String, password: String) {
        closeAll()
        _state.value = _state.value.copy(hosting = false, connected = false, room = room, name = name,
            ip = ip, password = password, status = "Menghubungkan…", error = null)
        scope.launch {
            try {
                val s = Socket()
                s.connect(java.net.InetSocketAddress(ip.trim(), SYNC_PORT), 3500)
                s.tcpNoDelay = true; s.keepAlive = true
                val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8))
                client = s; clientWriter = writer
                val id = java.util.UUID.randomUUID().toString()
                send(writer, JSONObject().put("type", "join").put("id", id).put("room", room.trim()).put("name", name.trim()).put("password", password))
                val first = reader.readLine() ?: error("Host menutup koneksi")
                val welcome = JSONObject(first)
                if (welcome.optString("type") != "welcome") error(welcome.optString("reason", "Ditolak host"))
                _state.value = _state.value.copy(connected = true, status = "Terhubung ke ${room.trim()}", error = null)
                heartbeat?.cancel()
                heartbeat = launch { while (isActive && client?.isClosed == false) { runCatching { send(writer, JSONObject().put("type", "ping").put("at", System.currentTimeMillis())) }; delay(HEARTBEAT_MS) } }
                while (true) {
                    val line = reader.readLine() ?: break
                    handleClientMessage(JSONObject(line))
                }
                if (_state.value.connected) {
                    _state.value = _state.value.copy(connected = false, playing = false, status = "Koneksi putus", error = "Koneksi ke host terputus")
                    stopLocalPlayback()
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(connected = false, status = "Gagal terhubung", error = e.message ?: "Periksa IP dan password")
            }
        }
    }

    private fun handleClientMessage(m: JSONObject) {
        when (m.optString("type")) {
            "members" -> {
                val a = m.optJSONArray("members")
                val list = mutableListOf<SyncMember>()
                if (a != null) for (i in 0 until a.length()) { val o = a.optJSONObject(i) ?: continue; list += SyncMember(o.optString("id"), o.optString("name"), volume = o.optDouble("volume", 1.0).toFloat()) }
                _state.value = _state.value.copy(members = list)
            }
            "sync" -> {
                val id = m.optString("trackId")
                val pos = m.optLong("position", 0L)
                val shouldPlay = m.optBoolean("playing", false)
                val local = RexPlayerController.state.value
                val needsRepair = id.isNotBlank() && (local.nowPlaying?.id != id || kotlin.math.abs(local.positionMs - pos) > 1700L || local.isPlaying != shouldPlay)
                if (needsRepair && playbackJob?.isActive != true) playbackJob = scope.launch { reconcileLocalTrack(id, pos, shouldPlay) }
                _state.value = _state.value.copy(trackId = id, trackTitle = m.optString("title", "Belum ada lagu"), positionMs = pos, playing = shouldPlay)
            }
            "state" -> {
                val track = m.optJSONObject("track")
                val id = track?.optString("id").orEmpty()
                val title = track?.optString("title").orEmpty().ifBlank { "Belum ada lagu" }
                val pos = m.optLong("position", 0L)
                val playing = m.optBoolean("playing", false)
                val startAt = m.optLong("startAt", 0L)
                _state.value = _state.value.copy(trackId = id, trackTitle = title, positionMs = pos, playing = playing, status = "Sinkronisasi · $title")
                if (id.isNotBlank()) playbackJob?.cancel(); if (id.isNotBlank()) playbackJob = scope.launch { syncLocalTrack(id, pos, playing, startAt) }
            }
            "stop" -> { playbackJob?.cancel(); stopLocalPlayback(); _state.value = _state.value.copy(playing = false, status = m.optString("reason", "Pemutaran dihentikan")) }
            "queue" -> {
                val arr = m.optJSONArray("items"); val list = mutableListOf<RexTrack>()
                if (arr != null) for (i in 0 until arr.length()) trackFromJson(arr.optJSONObject(i))?.let { list += it }
                _state.value = _state.value.copy(queue = list)
            }
            "queue_item" -> trackFromJson(m.optJSONObject("track"))?.let { t -> if (_state.value.queue.none { it.id == t.id }) _state.value = _state.value.copy(queue = _state.value.queue + t) }
            "volume" -> { val v = m.optDouble("value", 1.0).toFloat().coerceIn(0f, 1f); RexPlayerController.setSyncVolume(v) }
            "reject" -> _state.value = _state.value.copy(error = m.optString("reason"), status = "Akses ditolak")
        }
    }

    private suspend fun syncLocalTrack(id: String, pos: Long, playing: Boolean, startAt: Long) {
        RexPlayerController.init(appContextRef ?: return)
        val track = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == id }?.track
        if (track == null) {
            _state.value = _state.value.copy(status = "Lagu belum ada di HP ini", error = "Lagu '$id' belum diterima atau belum terindeks di library offline; tunggu transfer cache selesai.")
            return
        }
        withContext(Dispatchers.Main) { RexPlayerController.playFromOffline(track) }
        val deadline = System.currentTimeMillis() + 12000
        while (System.currentTimeMillis() < deadline) {
            val s = RexPlayerController.state.value
            if (s.nowPlaying?.id == id && s.durationMs > 0L && s.phase == LoadPhase.Idle) break
            delay(100)
        }
        val actual = RexPlayerController.state.value
        if (actual.nowPlaying?.id != id) return
        val targetWall = if (startAt > 0L) startAt else System.currentTimeMillis()
        val targetPos = max(0L, if (startAt > 0L) pos + max(0L, targetWall - startAt) else pos)
        delay(max(0L, targetWall - System.currentTimeMillis()))
        withContext(Dispatchers.Main) {
            RexPlayerController.seekToPosition(targetPos)
            if (playing) RexPlayerController.play() else RexPlayerController.pause()
        }
    }

    private suspend fun reconcileLocalTrack(id: String, targetPosition: Long, shouldPlay: Boolean) {
        val context = appContextRef ?: return
        RexPlayerController.init(context)
        val current = RexPlayerController.state.value
        if (current.nowPlaying?.id != id) {
            val track = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == id }?.track
            if (track == null) {
                _state.value = _state.value.copy(error = "Lagu belum tersedia offline di HP ini.")
                return
            }
            withContext(Dispatchers.Main) { RexPlayerController.playFromOffline(track) }
            val deadline = System.currentTimeMillis() + 10000L
            while (System.currentTimeMillis() < deadline) {
                val s = RexPlayerController.state.value
                if (s.nowPlaying?.id == id && s.durationMs > 0L) break
                delay(100L)
            }
        }
        val now = RexPlayerController.state.value
        if (now.nowPlaying?.id != id) return
        if (kotlin.math.abs(now.positionMs - targetPosition) > 900L) withContext(Dispatchers.Main) { RexPlayerController.seekToPosition(targetPosition) }
        if (shouldPlay && !now.isPlaying) withContext(Dispatchers.Main) { RexPlayerController.play() }
        if (!shouldPlay && now.isPlaying) withContext(Dispatchers.Main) { RexPlayerController.pause() }
    }

    private var appContextRef: Context? = null
    fun bindContext(context: Context) {
        appContextRef = context.applicationContext
        RexMusicService.start(context.applicationContext); RexOfflineManager.init(context.applicationContext); RexPlayerController.init(context.applicationContext)
        if (transferJob?.isActive != true) transferJob = scope.launch {
            try {
                val ss = ServerSocket(TRANSFER_PORT, 16, InetAddress.getByName("0.0.0.0")); transferServer = ss
                while (isActive && !ss.isClosed) {
                    val socket = ss.accept()
                    launch(Dispatchers.IO) { receiveBundle(socket) }
                }
            } catch (e: Exception) { if (transferServer != null) _state.value = _state.value.copy(cacheStatus = "Transfer: ${e.message}") }
        }
    }

    fun playTrack(track: RexTrack) {
        if (!_state.value.hosting) return
        val local = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == track.id }
        if (local == null) { _state.value = _state.value.copy(error = "Pilih lagu yang sudah tersedia offline di HP host."); return }
        hostTrack = local.track
        playAnchorPositionMs = 0L
        playAnchorWallMs = System.currentTimeMillis() + START_LEAD_MS
        hostPlaying = true
        _state.value = _state.value.copy(trackId = track.id, trackTitle = track.title, positionMs = 0L, playing = true, status = "Mulai serentak…", error = null)
        broadcastState()
        playbackJob?.cancel()
        playbackJob = scope.launch {
            delay(max(0L, playAnchorWallMs - System.currentTimeMillis()))
            withContext(Dispatchers.Main) { RexPlayerController.playFromOffline(local.track) }
            delay(600)
            withContext(Dispatchers.Main) { RexPlayerController.seekToPosition(0L); RexPlayerController.play() }
        }
    }

    private fun resumeRoomAtCurrentPosition() {
        val track = hostTrack ?: return
        playAnchorPositionMs = RexPlayerController.state.value.positionMs.coerceAtLeast(playAnchorPositionMs)
        playAnchorWallMs = System.currentTimeMillis() + START_LEAD_MS
        hostPlaying = true
        _state.value = _state.value.copy(playing = true, positionMs = playAnchorPositionMs, status = "Anggota kembali · sinkronisasi ulang", error = null)
        broadcastState()
        playbackJob?.cancel()
        playbackJob = scope.launch {
            delay(max(0L, playAnchorWallMs - System.currentTimeMillis()))
            withContext(Dispatchers.Main) {
                if (RexPlayerController.state.value.nowPlaying?.id != track.id) RexPlayerController.playFromOffline(track)
                delay(150L)
                RexPlayerController.seekToPosition(playAnchorPositionMs)
                RexPlayerController.play()
            }
        }
    }

    fun togglePlayback() {
        if (!_state.value.hosting) return
        hostPlaying = !hostPlaying
        if (hostPlaying) {
            playAnchorPositionMs = RexPlayerController.state.value.positionMs
            playAnchorWallMs = System.currentTimeMillis() + START_LEAD_MS
            broadcastState()
            playbackJob?.cancel(); playbackJob = scope.launch { delay(max(0L, playAnchorWallMs-System.currentTimeMillis())); withContext(Dispatchers.Main) { RexPlayerController.seekToPosition(playAnchorPositionMs); RexPlayerController.play() } }
        } else {
            scope.launch { withContextMain { RexPlayerController.pause() } }
            _state.value = _state.value.copy(playing = false, positionMs = RexPlayerController.state.value.positionMs, status = "Dijeda host")
            broadcastState()
        }
    }

    fun stopAll(reason: String = "Host menghentikan pemutaran") {
        hostPlaying = false; playbackJob?.cancel()
        scope.launch { withContextMain { RexPlayerController.pause() } }
        _state.value = _state.value.copy(playing = false, positionMs = RexPlayerController.state.value.positionMs, status = reason)
        peers.forEach { runCatching { send(it.writer, JSONObject().put("type", "stop").put("reason", reason)) } }
    }

    private fun sendState(p: Peer) {
        val track = hostTrack
        val pos = if (hostPlaying) playAnchorPositionMs + max(0L, System.currentTimeMillis() - playAnchorWallMs) else RexPlayerController.state.value.positionMs
        send(p.writer, JSONObject().put("type", "state").put("track", track?.let { JSONObject().put("id", it.id).put("title", it.title).put("artist", it.artist) })
            .put("position", pos).put("playing", hostPlaying).put("startAt", if (hostPlaying) max(System.currentTimeMillis()+500L, playAnchorWallMs) else 0L))
    }
    private fun broadcastState() { peers.forEach { sendState(it) } }
    private fun broadcastTick() {
        val track = hostTrack ?: return
        val current = RexPlayerController.state.value
        // RexPlayerController has its own autoplay. If it advances away from the shared track,
        // hand control back to the room queue so every member gets the same next track.
        if (hostPlaying && System.currentTimeMillis() > playAnchorWallMs + 2500L &&
            current.isPlaying && current.nowPlaying?.id != null && current.nowPlaying?.id != track.id) {
            val next = sharedQueue.firstOrNull()
            if (next != null) { playQueued(next); return }
            stopAll("Lagu selesai · antrean bersama kosong")
            return
        }
        val position = if (hostPlaying) {
            if (current.nowPlaying?.id == track.id && current.positionMs > 0L) current.positionMs
            else playAnchorPositionMs + max(0L, System.currentTimeMillis() - playAnchorWallMs)
        } else current.positionMs
        val msg = JSONObject().put("type", "sync").put("trackId", track.id).put("title", track.title)
            .put("position", position).put("playing", hostPlaying)
        peers.forEach { send(it.writer, msg) }
        _state.value = _state.value.copy(positionMs = position, playing = hostPlaying)
    }
    private fun broadcastMembers() {
        val a = org.json.JSONArray().put(JSONObject().put("id", "host").put("name", "Host"))
        peers.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("volume", it.volume)) }
        _state.value = _state.value.copy(members = listOf(SyncMember("host", "Host")) + peers.map { SyncMember(it.id, it.name, ip = it.ip, volume = it.volume) }, volumes = peers.associate { it.id to it.volume })
        val msg = JSONObject().put("type", "members").put("members", a)
        peers.forEach { send(it.writer, msg) }
    }
    fun addToQueue(track: RexTrack) {
        val local = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == track.id }
        if (local == null) { _state.value = _state.value.copy(error = "Lagu ini belum tersedia offline di HP kamu.", cacheStatus = "Pilih lagu yang sudah diunduh."); return }
        if (_state.value.hosting) {
            if (sharedQueue.none { it.id == local.track.id }) sharedQueue += local.track
            publishQueue()
            _state.value = _state.value.copy(cacheStatus = "Menyalin ${track.title} ke cache anggota…")
            scope.launch { distributeLocalTrack(local.track) }
        } else if (_state.value.connected) {
            runCatching { clientWriter?.let { send(it, JSONObject().put("type", "queue_add").put("track", trackJson(local.track))) } }
            scope.launch { sendBundle(_state.value.ip, local.track) }
            _state.value = _state.value.copy(cacheStatus = "Mengirim ${track.title} ke room…", error = null)
        }
    }

    fun playQueued(track: RexTrack) {
        if (_state.value.hosting) scope.launch {
            delay(500L)
            val offline = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == track.id }
            if (offline == null) { _state.value = _state.value.copy(error = "Transfer lagu belum selesai atau izin penyimpanan belum aktif."); return@launch }
            sharedQueue.removeAll { it.id == track.id }; publishQueue(); playTrack(offline.track)
        } else if (_state.value.connected) runCatching { clientWriter?.let { send(it, JSONObject().put("type", "play_request").put("trackId", track.id)) } }
    }

    fun setMemberVolume(id: String, value: Float) {
        val v = value.coerceIn(0f, 1f)
        if (!_state.value.hosting) return
        peers.firstOrNull { it.id == id }?.let { p -> p.volume = v; runCatching { send(p.writer, JSONObject().put("type", "volume").put("value", v)) } }
        _state.value = _state.value.copy(volumes = _state.value.volumes + (id to v), members = _state.value.members.map { if (it.id == id) it.copy(volume = v) else it })
        broadcastMembers()
    }

    private fun trackJson(t: RexTrack) = JSONObject().put("id", t.id).put("title", t.title).put("artist", t.artist).put("album", t.album).put("cover", t.cover).put("spotifyUrl", t.spotifyUrl).put("durationText", t.durationText)
    private fun trackFromJson(o: JSONObject?): RexTrack? = o?.let { RexTrack(it.optString("id"), it.optString("title"), it.optString("artist"), it.optString("album", "Single"), it.optString("cover"), spotifyUrl = it.optString("spotifyUrl"), durationText = it.optString("durationText")) }?.takeIf { it.id.isNotBlank() && it.title.isNotBlank() }
    private fun sendQueue(p: Peer) { val arr = org.json.JSONArray(); sharedQueue.forEach { arr.put(trackJson(it)) }; send(p.writer, JSONObject().put("type", "queue").put("items", arr)) }
    private fun publishQueue() { val arr = org.json.JSONArray(); sharedQueue.forEach { arr.put(trackJson(it)) }; val msg = JSONObject().put("type", "queue").put("items", arr); peers.forEach { send(it.writer, msg) }; _state.value = _state.value.copy(queue = sharedQueue.toList()) }

    private suspend fun distributeLocalTrack(track: RexTrack) {
        val entry = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == track.id } ?: return
        val peersSnapshot = peers.toList()
        for (p in peersSnapshot) {
            val ok = sendBundle(p.ip, track)
            if (!ok) _state.value = _state.value.copy(cacheStatus = "Gagal mengirim ke ${p.name}; cek Wi-Fi/firewall")
        }
        _state.value = _state.value.copy(cacheStatus = "Cache lagu siap untuk anggota yang terjangkau")
    }

    private suspend fun sendBundle(host: String, track: RexTrack): Boolean = withContext(Dispatchers.IO) {
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

    private fun receiveBundle(socket: Socket) {
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

    private fun send(w: BufferedWriter, m: JSONObject) { synchronized(w) { w.write(m.toString()); w.newLine(); w.flush() } }
    private suspend fun withContextMain(block: () -> Unit) { withContext(Dispatchers.Main) { block() } }

    fun disconnect() { closeAll(); _state.value = _state.value.copy(hosting = false, connected = false, playing = false, members = emptyList(), status = "Terputus", error = null) }
    private fun closeAll() {
        heartbeat?.cancel(); playbackJob?.cancel(); roomJob?.cancel(); roomJob = null
        sharedQueue.clear()
        peers.forEach { runCatching { it.socket.close() } }; peers.clear()
        runCatching { client?.close() }; client = null; clientWriter = null
        runCatching { server?.close() }; server = null
        if (_state.value.hosting) stopLocalPlayback()
        _state.value = _state.value.copy(queue = emptyList())
    }
    private fun stopLocalPlayback() { appContextRef?.let { c -> scope.launch(Dispatchers.Main) { runCatching { RexPlayerController.init(c); RexPlayerController.pause() } } } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexMusicSyncScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val state by RexMusicSyncEngine.state.collectAsState()
    val music by RexPlayerController.state.collectAsState()
    val offlineState by RexOfflineManager.state.collectAsState()
    var room by remember { mutableStateOf("RexSyncRoom1") }
    var ip by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("rexsync") }
    var name by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { RexMusicSyncEngine.bindContext(context); ip = RexMusicSyncEngine.detectIp(context) }
    Scaffold(topBar = { TopAppBar(title = { Text("RexMusicSync", fontWeight = FontWeight.SemiBold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text("Dengarkan bareng", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                        Text("Sinkronisasi melalui Wi-Fi lokal. Semua perangkat harus berada di jaringan yang sama.", style = MaterialTheme.typography.bodyMedium)
                        Text("Status: ${state.status}", style = MaterialTheme.typography.labelLarge)
                        if (state.cacheStatus.isNotBlank()) Text(state.cacheStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
            item {
                Text("BUAT ROOM · HOST", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(room, { room = it }, label = { Text("Nama room") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(ip, { ip = it }, label = { Text("IP Wi-Fi host (otomatis) ") }, modifier = Modifier.fillMaxWidth(), singleLine = true, trailingIcon = { TextButton(onClick = { ip = RexMusicSyncEngine.detectIp(context) }) { Text("Auto") } })
                OutlinedTextField(password, { password = it }, label = { Text("Password room") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(onClick = { RexMusicSyncEngine.createRoom(room, password, ip) }, enabled = !state.hosting && ip.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(if (state.hosting) "Room aktif" else "Buat room") }
            }
            item { HorizontalDivider(); Text("GABUNG ROOM · USER", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
            item {
                OutlinedTextField(name, { name = it }, label = { Text("Nama kamu") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(room, { room = it }, label = { Text("Nama room") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(ip, { ip = it }, label = { Text("IP host") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(password, { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(onClick = { RexMusicSyncEngine.joinRoom(room, name.ifBlank { "User" }, ip, password) }, enabled = !state.connected && ip.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Gabung room") }
            }
            if (state.hosting || state.connected) {
                item { HorizontalDivider(); Text("ANGGOTA (${state.members.size})", style = MaterialTheme.typography.labelLarge) }
                items(state.members, key = { it.id }) { member -> ListItem(headlineContent = { Text(member.name) }, supportingContent = { Text(if (member.id == "host") "Host" else "Terhubung") }, leadingContent = { Icon(Icons.Default.MusicNote, null) }) }
            }
            if (state.hosting || state.connected) {
                item { HorizontalDivider(); Text("ANTREAN BERSAMA (${state.queue.size})", style = MaterialTheme.typography.labelLarge); if (state.cacheStatus.isNotBlank()) Text(state.cacheStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                items(state.queue, key = { "q-${it.id}" }) { track ->
                    Card(onClick = { RexMusicSyncEngine.playQueued(track) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.QueueMusic, null); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(track.title, fontWeight = FontWeight.SemiBold); Text(track.artist, style = MaterialTheme.typography.bodySmall) }; Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary) }
                    }
                }
                if (state.hosting) items(state.members.filter { it.id != "host" }, key = { "vol-${it.id}" }) { member ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("Volume · ${member.name}", style = MaterialTheme.typography.labelMedium); Slider(value = state.volumes[member.id] ?: member.volume, onValueChange = { RexMusicSyncEngine.setMemberVolume(member.id, it) }, valueRange = 0f..1f) }
                }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { OutlinedButton(onClick = { RexMusicSyncEngine.togglePlayback() }, modifier = Modifier.weight(1f), enabled = state.hosting) { Icon(if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text(if (state.playing) "Pause semua" else "Lanjutkan") }; OutlinedButton(onClick = { RexMusicSyncEngine.stopAll() }, modifier = Modifier.weight(1f), enabled = state.hosting) { Icon(Icons.Default.Stop, null); Spacer(Modifier.width(6.dp)); Text("Stop semua") } } }
            }
            item { HorizontalDivider(); Text("LAGU OFFLINE DI HP INI", style = MaterialTheme.typography.labelLarge); Text("Tambah lagu akan langsung mengirim cache ke room, tidak menunggu lagu diputar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(offlineState.entries, key = { "offline-${it.track.id}" }) { entry ->
                Card(onClick = { RexMusicSyncEngine.addToQueue(entry.track) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.MusicNote, null); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(entry.track.title, fontWeight = FontWeight.SemiBold); Text(entry.track.artist, style = MaterialTheme.typography.bodySmall) }; Text("+", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall) }
                }
            }
            if (state.connected && !state.hosting) item { OutlinedButton(onClick = { RexMusicSyncEngine.disconnect() }, modifier = Modifier.fillMaxWidth()) { Text("Keluar dari room") } }
            if (state.hosting || state.connected) item { TextButton(onClick = { RexMusicSyncEngine.disconnect() }, modifier = Modifier.fillMaxWidth()) { Text("Tutup koneksi") } }
        }
    }
}
