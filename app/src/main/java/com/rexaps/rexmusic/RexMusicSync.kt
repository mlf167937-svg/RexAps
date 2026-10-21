package com.rexaps.rexmusic

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.net.wifi.WifiManager
import android.text.format.Formatter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
private const val START_LEAD_MS = 3000L
private const val DRIFT_CORRECTION_MS = 250L

private data class SyncMember(val id: String, val name: String, val connectedAt: Long = SystemClock.elapsedRealtime(), val ip: String = "", val volume: Float = 1f, val deviceVolume: Float = -1f)
private data class SyncUiState(
    val hosting: Boolean = false, val connected: Boolean = false, val room: String = "RexSyncRoom1",
    val ip: String = "", val password: String = "rexsync", val name: String = "User",
    val status: String = "Belum terhubung", val members: List<SyncMember> = emptyList(),
    val trackId: String = "", val trackTitle: String = "Belum ada lagu", val positionMs: Long = 0L,
    val playing: Boolean = false, val error: String? = null, val queue: List<RexTrack> = emptyList(), val cacheStatus: String = "", val volumes: Map<String, Float> = emptyMap(), val hostVolume: Float = 1f, val roomElapsedMs: Long = 0L
)

/** Local Wi-Fi sync protocol. Uses newline-delimited JSON control messages; no SSH or internet. */
private object RexMusicSyncEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, error ->
        _state.value = _state.value.copy(error = "RexMusicSync: ${error.message ?: "terjadi kesalahan"}")
    })
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
    private var playAnchorWallMs = 0L // host monotonic elapsedRealtime timestamp
    private var roomStartedElapsedMs = 0L
    @Volatile private var clockOffsetMs = 0L // host elapsedRealtime - client elapsedRealtime
    @Volatile private var lastClockRttMs = -1L
    private var playAnchorPositionMs = 0L
    private var hostPlaying = false
    private var hostPreparing = false
    private var resumeWhenMemberReturns = false

    private data class Peer(val socket: Socket, val reader: BufferedReader, val writer: BufferedWriter,
                            var id: String = "", var name: String = "", @Volatile var lastSeen: Long = SystemClock.elapsedRealtime(), val ip: String = socket.inetAddress?.hostAddress.orEmpty(), @Volatile var volume: Float = 1f, @Volatile var deviceVolume: Float = -1f, @Volatile var clockSynced: Boolean = false)

    fun detectIp(context: Context): String = runCatching {
        // Works for Wi-Fi client and many Android hotspot interfaces; avoid VPN/tunnel interfaces.
        val interfaces = java.net.NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        val candidates = interfaces.filter { ni ->
            ni.isUp && !ni.isLoopback && !ni.name.lowercase().matches(Regex(".*(tun|tap|wg|warp|vpn|dummy).*"))
        }.flatMap { ni -> ni.inetAddresses.toList().filterIsInstance<java.net.Inet4Address>()
            .filter { !it.isLoopbackAddress && !it.isLinkLocalAddress }
            .map { ni.name.lowercase() to it.hostAddress.orEmpty() } }
        candidates.firstOrNull { (name, ip) -> name.matches(Regex(".*(wlan|wifi|ap|swlan|p2p|eth).*")) && ip.isNotBlank() }?.second
            ?: candidates.firstOrNull { it.second.isNotBlank() }?.second.orEmpty()
    }.getOrDefault("")

    fun createRoom(room: String, password: String, ip: String) {
        closeAll()
        roomName = room.trim().ifBlank { "RexSyncRoom1" }
        roomStartedElapsedMs = SystemClock.elapsedRealtime()
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
            p.lastSeen = SystemClock.elapsedRealtime()
            peers.add(p)
            send(p.writer, JSONObject().put("type", "welcome").put("id", p.id).put("room", roomName))
            broadcastMembers()
            sendQueue(p); broadcastMembers()
            while (true) {
                val line = withContext(Dispatchers.IO) { p.reader.readLine() } ?: break
                val msg = JSONObject(line)
                when (msg.optString("type")) {
                    "ping" -> {
                        p.lastSeen = SystemClock.elapsedRealtime()
                        p.deviceVolume = msg.optDouble("deviceVolume", -1.0).toFloat().coerceIn(-1f, 1f)
                        _state.value = _state.value.copy(members = _state.value.members.map { if (it.id == p.id) it.copy(deviceVolume = p.deviceVolume) else it })
                        val hostSend = SystemClock.elapsedRealtime()
                        p.clockSynced = true
                        send(p.writer, JSONObject().put("type", "pong")
                            .put("clientElapsed", msg.optLong("clientElapsed", 0L))
                            .put("hostReceiveElapsed", hostSend)
                            .put("hostSendElapsed", SystemClock.elapsedRealtime()))
                        if (hostTrack != null) sendState(p)
                    }
                    "ready" -> { p.lastSeen = SystemClock.elapsedRealtime() }
                    "request_track" -> { p.lastSeen = SystemClock.elapsedRealtime(); if (hostTrack != null) sendState(p) }
                    "queue_add" -> {
                        p.lastSeen = SystemClock.elapsedRealtime()
                        val t = trackFromJson(msg.optJSONObject("track"))
                        if (t != null && sharedQueue.none { it.id == t.id }) { sharedQueue += t; publishQueue() }
                        _state.value = _state.value.copy(cacheStatus = "Menerima ${t?.title ?: "lagu"} dari ${p.name}…")
                    }
                    "queue_request" -> { p.lastSeen = SystemClock.elapsedRealtime(); sendQueue(p) }
                    "volume" -> { p.lastSeen = SystemClock.elapsedRealtime(); p.volume = msg.optDouble("value", 1.0).toFloat().coerceIn(0f, 1f) }
                    "play_request" -> { p.lastSeen = SystemClock.elapsedRealtime(); val id = msg.optString("trackId"); sharedQueue.firstOrNull { it.id == id }?.let { queued -> val local = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == id }; if (local != null) { sharedQueue.removeAll { it.id == id }; publishQueue(); playTrack(local.track) } } }
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
            val now = SystemClock.elapsedRealtime()
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
                heartbeat = launch { while (isActive && client?.isClosed == false) { runCatching {
                        val am = appContextRef?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                        val maxVolume = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0
                        val currentVolume = am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                        val actual = if (maxVolume > 0) currentVolume.toFloat() / maxVolume else -1f
                        send(writer, JSONObject().put("type", "ping").put("clientElapsed", SystemClock.elapsedRealtime()).put("deviceVolume", actual))
                    }; delay(HEARTBEAT_MS) } }
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
            "room_time" -> _state.value = _state.value.copy(roomElapsedMs = m.optLong("roomElapsedMs", _state.value.roomElapsedMs))
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
                if (a != null) for (i in 0 until a.length()) { val o = a.optJSONObject(i) ?: continue; list += SyncMember(o.optString("id"), o.optString("name"), volume = o.optDouble("volume", 1.0).toFloat().coerceIn(0f, 1f), deviceVolume = o.optDouble("deviceVolume", -1.0).toFloat().coerceIn(-1f, 1f)) }
                _state.value = _state.value.copy(members = list)
            }
            "sync" -> {
                val id = m.optString("trackId")
                val pos = m.optLong("position", 0L)
                val hostAt = m.optLong("hostElapsed", 0L)
                val shouldPlay = m.optBoolean("playing", false)
                val local = RexPlayerController.state.value
                val projected = if (shouldPlay && hostAt > 0L) pos + (SystemClock.elapsedRealtime() + clockOffsetMs - hostAt).coerceAtLeast(0L) else pos
                val drift = kotlin.math.abs(local.positionMs - projected)
                val needsTrack = id.isNotBlank() && local.nowPlaying?.id != id
                val needsPlayState = id.isNotBlank() && local.nowPlaying?.id == id && local.isPlaying != shouldPlay
                if (needsTrack && playbackJob?.isActive != true) playbackJob = scope.launch { syncLocalTrack(id, pos, shouldPlay, m.optLong("startAt", 0L)) }
                else if (needsPlayState && playbackJob?.isActive != true) playbackJob = scope.launch { reconcileLocalTrack(id, projected, shouldPlay) }
                else if (shouldPlay && drift > DRIFT_CORRECTION_MS && playbackJob?.isActive != true) {
                    playbackJob = scope.launch { withContext(Dispatchers.Main) { RexPlayerController.seekToPositionImmediate(projected) } }
                }
                _state.value = _state.value.copy(trackId = id, trackTitle = m.optString("title", "Belum ada lagu"), positionMs = projected, playing = shouldPlay, roomElapsedMs = m.optLong("roomElapsedMs", _state.value.roomElapsedMs))
            }
            "state" -> {
                val track = m.optJSONObject("track")
                val id = track?.optString("id").orEmpty()
                val title = track?.optString("title").orEmpty().ifBlank { "Belum ada lagu" }
                val pos = m.optLong("position", 0L)
                val playing = m.optBoolean("playing", false)
                val startAt = m.optLong("startAt", 0L)
                _state.value = _state.value.copy(trackId = id, trackTitle = title, positionMs = pos, playing = playing, roomElapsedMs = m.optLong("roomElapsedMs", _state.value.roomElapsedMs), status = "Sinkronisasi · $title")

                // A state snapshot may arrive after the initial sync packet. Never restart
                // preparation for a track that is already loaded, and don't cancel an
                // in-flight preparation for the same track: doing so can keep members silent.
                if (id.isNotBlank()) {
                    val local = RexPlayerController.state.value
                    val sameTrackLoaded = local.nowPlaying?.id == id && local.durationMs > 0L
                    val preparationForSameTrack = playbackJob?.isActive == true && local.nowPlaying?.id == id
                    if (!sameTrackLoaded && !preparationForSameTrack) {
                        playbackJob?.cancel()
                        playbackJob = scope.launch { syncLocalTrack(id, pos, playing, startAt) }
                    } else if (sameTrackLoaded && local.isPlaying != playing && playbackJob?.isActive != true) {
                        playbackJob = scope.launch { reconcileLocalTrack(id, pos, playing) }
                    }
                }
            }
            "stop" -> { playbackJob?.cancel(); stopLocalPlayback(); _state.value = _state.value.copy(playing = false, status = m.optString("reason", "Pemutaran dihentikan")) }
            "queue" -> {
                val arr = m.optJSONArray("items"); val list = mutableListOf<RexTrack>()
                if (arr != null) for (i in 0 until arr.length()) trackFromJson(arr.optJSONObject(i))?.let { list += it }
                _state.value = _state.value.copy(queue = list)
            }
            "queue_item" -> trackFromJson(m.optJSONObject("track"))?.let { t -> if (_state.value.queue.none { it.id == t.id }) _state.value = _state.value.copy(queue = _state.value.queue + t) }
            "volume" -> { val v = m.optDouble("value", 1.0).toFloat().coerceIn(0f, 1f); scope.launch(Dispatchers.Main) { RexPlayerController.setSyncVolume(v) } }
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
        withContext(Dispatchers.Main) { RexPlayerController.prepareFromOfflineForSync(track) }
        val deadline = SystemClock.elapsedRealtime() + 12000
        while (SystemClock.elapsedRealtime() < deadline) {
            val s = RexPlayerController.state.value
            if (s.nowPlaying?.id == id && s.durationMs > 0L && s.phase == LoadPhase.Idle) break
            delay(100)
        }
        val actual = RexPlayerController.state.value
        if (actual.nowPlaying?.id != id) return
        val targetHostElapsed = if (startAt > 0L) startAt else SystemClock.elapsedRealtime() + clockOffsetMs
        val targetLocalElapsed = targetHostElapsed - clockOffsetMs
        delay(max(0L, targetLocalElapsed - SystemClock.elapsedRealtime()))
        val targetPos = max(0L, pos + max(0L, SystemClock.elapsedRealtime() + clockOffsetMs - targetHostElapsed))
        withContext(Dispatchers.Main) {
            if (playing) RexPlayerController.startSyncPlayback(targetPos)
            else { RexPlayerController.seekToPositionImmediate(pos); RexPlayerController.pause() }
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
            val deadline = SystemClock.elapsedRealtime() + 10000L
            while (SystemClock.elapsedRealtime() < deadline) {
                val s = RexPlayerController.state.value
                if (s.nowPlaying?.id == id && s.durationMs > 0L) break
                delay(100L)
            }
        }
        val now = RexPlayerController.state.value
        if (now.nowPlaying?.id != id) return
        if (kotlin.math.abs(now.positionMs - targetPosition) > DRIFT_CORRECTION_MS) withContext(Dispatchers.Main) { RexPlayerController.seekToPositionImmediate(targetPosition) }
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

    private fun resumeRoomAtCurrentPosition() {
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

    fun togglePlayback() {
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

    fun stopAll(reason: String = "Host menghentikan pemutaran") {
        hostPreparing = false; hostPlaying = false; playbackJob?.cancel()
        scope.launch { withContextMain { RexPlayerController.pause() } }
        _state.value = _state.value.copy(playing = false, positionMs = RexPlayerController.state.value.positionMs, status = reason)
        peers.forEach { runCatching { send(it.writer, JSONObject().put("type", "stop").put("reason", reason)) } }
    }

    private fun sendState(p: Peer) {
        val track = hostTrack
        val now = SystemClock.elapsedRealtime()
        val startAt = if (hostPlaying) max(now + START_LEAD_MS, playAnchorWallMs) else 0L
        val currentPosition = if (hostPlaying) playAnchorPositionMs + max(0L, now - playAnchorWallMs) else RexPlayerController.state.value.positionMs
        // If playback is already underway, project to the future start; if the anchor is still ahead, keep its start position.
        val pos = if (hostPlaying) {
            if (now < playAnchorWallMs) playAnchorPositionMs else currentPosition + max(0L, startAt - now)
        } else currentPosition
        send(p.writer, JSONObject().put("type", "state").put("track", track?.let { JSONObject().put("id", it.id).put("title", it.title).put("artist", it.artist) })
            .put("position", pos).put("playing", hostPlaying).put("hostElapsed", now).put("roomElapsedMs", (now - roomStartedElapsedMs).coerceAtLeast(0L)).put("startAt", startAt))
    }
    private fun broadcastState() { peers.filter { it.clockSynced }.forEach { sendState(it) } }
    private fun broadcastTick() {
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
    private fun broadcastMembers() {
        val a = org.json.JSONArray().put(JSONObject().put("id", "host").put("name", "Host"))
        peers.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("volume", it.volume).put("deviceVolume", it.deviceVolume)) }
        _state.value = _state.value.copy(members = listOf(SyncMember("host", "Host")) + peers.map { SyncMember(it.id, it.name, ip = it.ip, volume = it.volume, deviceVolume = it.deviceVolume) }, volumes = peers.associate { it.id to it.volume })
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

    fun setHostVolume(value: Float) {
        val v = value.coerceIn(0f, 1f)
        _state.value = _state.value.copy(hostVolume = v)
        appContextRef?.let { context ->
            RexPlayerController.init(context)
            RexPlayerController.setSyncVolume(v)
        }
    }

    fun setMemberVolume(id: String, value: Float) {
        val v = value.coerceIn(0f, 1f)
        if (!_state.value.hosting) return
        peers.firstOrNull { it.id == id }?.let { p -> p.volume = v; runCatching { send(p.writer, JSONObject().put("type", "volume").put("value", v)) } }
        _state.value = _state.value.copy(volumes = _state.value.volumes + (id to v), members = _state.value.members.map { if (it.id == id) it.copy(volume = v) else it })
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

    private fun send(w: BufferedWriter, m: JSONObject) {
        // Socket writes may fail when a phone disconnects mid-gesture. Never let an IOException
        // escape a Compose callback (especially the volume slider) and crash the whole screen.
        runCatching { synchronized(w) { w.write(m.toString()); w.newLine(); w.flush() } }
    }
    private suspend fun withContextMain(block: () -> Unit) { withContext(Dispatchers.Main) { block() } }

    fun disconnect() { closeAll(); _state.value = _state.value.copy(hosting = false, connected = false, playing = false, members = emptyList(), status = "Terputus", error = null) }
    private fun closeAll() {
        hostPreparing = false; hostPlaying = false; roomStartedElapsedMs = 0L; clockOffsetMs = 0L; lastClockRttMs = -1L
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

private fun formatRoomTime(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) / 1000L)
    return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", total / 3600L, (total / 60L) % 60L, total % 60L)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexMusicSyncScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val state by RexMusicSyncEngine.state.collectAsState()
    val offlineState by RexOfflineManager.state.collectAsState()

    var room by rememberSaveable { mutableStateOf("RexSyncRoom1") }
    var ip by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("rexsync") }
    var name by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var onlineResults by remember { mutableStateOf<List<RexTrack>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchMessage by rememberSaveable { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val active = state.hosting || state.connected
    val connecting = !active &&
        state.error == null &&
        state.status.lowercase().let {
            "menghubungkan" in it || "connecting" in it || "mencari host" in it
        }

    LaunchedEffect(Unit) {
        RexMusicSyncEngine.bindContext(context)
        if (ip.isBlank()) ip = RexMusicSyncEngine.detectIp(context)
    }

    val searchOnline: () -> Unit = {
        if (!searching && query.isNotBlank()) {
            searching = true
            searchMessage = "Mencari lagu…"
            scope.launch {
                val result = RexMusicApi.search(query.trim())
                onlineResults = result.getOrElse { emptyList() }
                searchMessage = if (result.isSuccess) {
                    "${onlineResults.size} hasil · unduh dulu agar bisa dipakai di room"
                } else {
                    result.exceptionOrNull()?.message ?: "Pencarian gagal"
                }
                searching = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("RexMusicSync", fontWeight = FontWeight.Bold)
                        Text(
                            "Listen together · local Wi-Fi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val useTwoPaneLayout = maxWidth >= 680.dp && maxWidth > maxHeight

            if (useTwoPaneLayout) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                    ) {
                        item { SyncOverviewCard(state = state, active = active, connecting = connecting) }
                        item {
                            SyncConnectionCard(
                                room = room,
                                onRoomChange = { room = it },
                                ip = ip,
                                onIpChange = { ip = it },
                                password = password,
                                onPasswordChange = { password = it },
                                name = name,
                                onNameChange = { name = it },
                                active = active,
                                onAutoIp = { ip = RexMusicSyncEngine.detectIp(context) },
                                onCreateRoom = { RexMusicSyncEngine.createRoom(room, password, ip) },
                                onJoinRoom = {
                                    RexMusicSyncEngine.joinRoom(
                                        room.trim(),
                                        name.ifBlank { "User" }.trim(),
                                        ip.trim(),
                                        password
                                    )
                                }
                            )
                        }
                        item {
                            SyncOnlineSearchCard(
                                query = query,
                                onQueryChange = { query = it },
                                searching = searching,
                                searchMessage = searchMessage,
                                onSearch = searchOnline
                            )
                        }
                        items(
                            onlineResults.distinctBy { it.id }.filter { it.id.isNotBlank() },
                            key = { "search-${it.id}" }
                        ) { track ->
                            SyncOnlineResultCard(
                                track = track,
                                downloaded = offlineState.entries.any { it.track.id == track.id },
                                onDownload = { RexOfflineManager.download(track) }
                            )
                        }
                        item {
                            SyncOfflineHeader(
                                count = offlineState.entries.distinctBy { it.track.id }.size,
                                active = active
                            )
                        }
                        if (offlineState.entries.distinctBy { it.track.id }.none { it.track.id.isNotBlank() }) {
                            item { SyncEmptyOfflineCard() }
                        }
                        items(
                            offlineState.entries.distinctBy { it.track.id }
                                .filter { it.track.id.isNotBlank() },
                            key = { "offline-${it.track.id}" }
                        ) { entry ->
                            SyncOfflineTrackCard(
                                track = entry.track,
                                active = active,
                                onQueue = { RexMusicSyncEngine.addToQueue(entry.track) }
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                    ) {
                        if (active) {
                            item { SyncMembersHeader(state.members.distinctBy { it.id }.size) }
                            items(
                                state.members.distinctBy { it.id },
                                key = { "member-${it.id}" }
                            ) { member ->
                                SyncMemberCard(
                                    member = member,
                                    state = state,
                                    onVolumeChange = { value ->
                                        RexMusicSyncEngine.setMemberVolume(member.id, value)
                                    }
                                )
                            }
                            item {
                                SyncPlaybackCard(
                                    state = state,
                                    onHostVolumeChange = { RexMusicSyncEngine.setHostVolume(it) },
                                    onTogglePlayback = { RexMusicSyncEngine.togglePlayback() },
                                    onStop = { RexMusicSyncEngine.stopAll() }
                                )
                            }
                            item { SyncQueueHeader(state.queue.size) }
                            if (state.queue.distinctBy { it.id }.isEmpty()) {
                                item { SyncEmptyQueueCard() }
                            }
                            items(
                                state.queue.distinctBy { it.id },
                                key = { "queue-${it.id}" }
                            ) { track ->
                                SyncQueueTrackCard(
                                    track = track,
                                    onPlay = { RexMusicSyncEngine.playQueued(track) }
                                )
                            }
                            item {
                                OutlinedButton(
                                    onClick = { RexMusicSyncEngine.disconnect() },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(if (state.hosting) "Tutup room" else "Keluar dari room")
                                }
                            }
                        } else {
                            item { SyncWaitingCard(connecting = connecting, error = state.error) }
                            item { SyncMembersEmptyCard() }
                            item { SyncPlaybackEmptyCard() }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 30.dp)
                ) {
                    item { SyncOverviewCard(state = state, active = active, connecting = connecting) }
                    item {
                        SyncConnectionCard(
                            room = room,
                            onRoomChange = { room = it },
                            ip = ip,
                            onIpChange = { ip = it },
                            password = password,
                            onPasswordChange = { password = it },
                            name = name,
                            onNameChange = { name = it },
                            active = active,
                            onAutoIp = { ip = RexMusicSyncEngine.detectIp(context) },
                            onCreateRoom = { RexMusicSyncEngine.createRoom(room, password, ip) },
                            onJoinRoom = {
                                RexMusicSyncEngine.joinRoom(
                                    room,
                                    name.ifBlank { "User" },
                                    ip,
                                    password
                                )
                            }
                        )
                    }

                    if (active) {
                        item { SyncMembersHeader(state.members.distinctBy { it.id }.size) }
                        items(
                            state.members.distinctBy { it.id },
                            key = { "member-${it.id}" }
                        ) { member ->
                            SyncMemberCard(
                                member = member,
                                state = state,
                                onVolumeChange = { value ->
                                    RexMusicSyncEngine.setMemberVolume(member.id, value)
                                }
                            )
                        }
                        item {
                            SyncPlaybackCard(
                                state = state,
                                onHostVolumeChange = { RexMusicSyncEngine.setHostVolume(it) },
                                onTogglePlayback = { RexMusicSyncEngine.togglePlayback() },
                                onStop = { RexMusicSyncEngine.stopAll() }
                            )
                        }
                        item { SyncQueueHeader(state.queue.size) }
                        if (state.queue.distinctBy { it.id }.isEmpty()) {
                            item { SyncEmptyQueueCard() }
                        }
                        items(
                            state.queue.distinctBy { it.id },
                            key = { "queue-${it.id}" }
                        ) { track ->
                            SyncQueueTrackCard(
                                track = track,
                                onPlay = { RexMusicSyncEngine.playQueued(track) }
                            )
                        }
                        item {
                            OutlinedButton(
                                onClick = { RexMusicSyncEngine.disconnect() },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(if (state.hosting) "Tutup room" else "Keluar dari room")
                            }
                        }
                    } else {
                        item { SyncWaitingCard(connecting = connecting, error = state.error) }
                        item { SyncMembersEmptyCard() }
                        item { SyncPlaybackEmptyCard() }
                    }

                    item {
                        SyncOnlineSearchCard(
                            query = query,
                            onQueryChange = { query = it },
                            searching = searching,
                            searchMessage = searchMessage,
                            onSearch = searchOnline
                        )
                    }
                    items(
                        onlineResults.distinctBy { it.id }.filter { it.id.isNotBlank() },
                        key = { "search-${it.id}" }
                    ) { track ->
                        SyncOnlineResultCard(
                            track = track,
                            downloaded = offlineState.entries.any { it.track.id == track.id },
                            onDownload = { RexOfflineManager.download(track) }
                        )
                    }
                    item {
                        SyncOfflineHeader(
                            count = offlineState.entries.distinctBy { it.track.id }.size,
                            active = active
                        )
                    }
                    if (offlineState.entries.distinctBy { it.track.id }.none { it.track.id.isNotBlank() }) {
                        item { SyncEmptyOfflineCard() }
                    }
                    items(
                        offlineState.entries.distinctBy { it.track.id }
                            .filter { it.track.id.isNotBlank() },
                        key = { "offline-${it.track.id}" }
                    ) { entry ->
                        SyncOfflineTrackCard(
                            track = entry.track,
                            active = active,
                            onQueue = { RexMusicSyncEngine.addToQueue(entry.track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncOverviewCard(
    state: SyncUiState,
    active: Boolean,
    connecting: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = scheme.primaryContainer
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = scheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp).size(28.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Music, in sync.",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "Putar bareng di jaringan Wi-Fi yang sama",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                val statusColor = when {
                    state.error != null -> scheme.errorContainer
                    active -> scheme.tertiaryContainer
                    else -> scheme.surface
                }
                val statusTextColor = when {
                    state.error != null -> scheme.onErrorContainer
                    active -> scheme.onTertiaryContainer
                    else -> scheme.onSurfaceVariant
                }
                Surface(shape = RoundedCornerShape(50), color = statusColor) {
                    Text(
                        when {
                            state.error != null -> "ERROR"
                            state.hosting -> "HOST"
                            state.connected -> "CONNECTED"
                            connecting -> "CONNECTING"
                            else -> "OFFLINE"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusTextColor
                    )
                }
            }

            Text(
                state.status,
                style = MaterialTheme.typography.titleSmall,
                color = if (state.error != null) scheme.error else scheme.onSurface
            )
            if (connecting) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Mencoba menghubungkan…", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
            if (active) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("ROOM", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                        Text(state.room, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("MEMBERS", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                        Text(
                            "${state.members.distinctBy { it.id }.size}",
                            style = MaterialTheme.typography.titleLarge,
                            color = scheme.primary
                        )
                    }
                    if (state.roomElapsedMs > 0L) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("ROOM TIME", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                            Text(formatRoomTime(state.roomElapsedMs), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
            if (state.cacheStatus.isNotBlank()) {
                Text(state.cacheStatus, style = MaterialTheme.typography.bodySmall, color = scheme.tertiary)
            }
            state.error?.let { errorText ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.errorContainer
                ) {
                    Text(
                        errorText,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        color = scheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncConnectionCard(
    room: String,
    onRoomChange: (String) -> Unit,
    ip: String,
    onIpChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    active: Boolean,
    onAutoIp: () -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SyncSectionHeading(title = "Connection", subtitle = "Host a room or join a device on your local Wi-Fi")
            OutlinedTextField(
                value = room,
                onValueChange = onRoomChange,
                label = { Text("Nama room") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !active
            )
            OutlinedTextField(
                value = ip,
                onValueChange = onIpChange,
                label = { Text("IP Wi-Fi host") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !active,
                trailingIcon = {
                    TextButton(onClick = onAutoIp, enabled = !active) { Text("AUTO") }
                }
            )
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password room") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !active
            )
            Button(
                onClick = onCreateRoom,
                enabled = !active && ip.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Buat room sebagai host")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Join an existing room", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Nama kamu") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !active
            )
            Button(
                onClick = onJoinRoom,
                enabled = !active && ip.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Gabung room")
            }
            Text(
                "Gunakan Wi-Fi yang sama atau hotspot host. Matikan VPN/WARP jika perangkat tidak bisa saling terhubung.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (active) {
                Text(
                    "Koneksi aktif. Putuskan room terlebih dahulu untuk mengganti detail koneksi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun SyncSectionHeading(
    title: String,
    subtitle: String? = null,
    titleColor: androidx.compose.ui.graphics.Color? = null,
    subtitleColor: androidx.compose.ui.graphics.Color? = null
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor ?: scheme.primary)
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor ?: scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SyncMembersHeader(count: Int) {
    SyncSectionHeading("Connected members", "$count device${if (count == 1) "" else "s"} in this room")
}

@Composable
private fun SyncMemberCard(
    member: SyncMember,
    state: SyncUiState,
    onVolumeChange: (Float) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = scheme.secondaryContainer
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = scheme.onSecondaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(member.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(
                        if (member.id == "host") "Host device" else "Connected · ${member.ip}",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (member.id == "host") {
                    Text("YOU", style = MaterialTheme.typography.labelSmall, color = scheme.primary)
                }
            }
            if (state.hosting && member.id != "host") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("RexSync gain", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    Text(
                        "${((state.volumes[member.id] ?: member.volume).coerceIn(0f, 1f) * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.primary
                    )
                }
                Slider(
                    value = (state.volumes[member.id] ?: member.volume).coerceIn(0f, 1f),
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f
                )
                Text(
                    if (member.deviceVolume >= 0f) {
                        "Media volume: ${(member.deviceVolume * 100).toInt()}% · app gain is separate"
                    } else {
                        "Waiting for this device to report media volume…"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            } else if (member.id != "host" && member.deviceVolume >= 0f) {
                Text(
                    "Media volume: ${(member.deviceVolume * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SyncPlaybackCard(
    state: SyncUiState,
    onHostVolumeChange: (Float) -> Unit,
    onTogglePlayback: () -> Unit,
    onStop: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SyncSectionHeading("Playback synchronization", "Shared playback status for this room", scheme.onPrimaryContainer, scheme.onPrimaryContainer.copy(alpha = 0.8f))
            Text(
                state.trackTitle.ifBlank { "Belum ada lagu" },
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onPrimaryContainer,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = scheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        if (state.playing) "PLAYING" else "PAUSED",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (state.roomElapsedMs > 0L) formatRoomTime(state.roomElapsedMs) else "Room timer ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            if (state.hosting) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Host volume", style = MaterialTheme.typography.labelMedium, color = scheme.onPrimaryContainer, modifier = Modifier.weight(1f))
                    Text("${(state.hostVolume.coerceIn(0f, 1f) * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = scheme.onPrimaryContainer)
                }
                Slider(
                    value = state.hostVolume.coerceIn(0f, 1f),
                    onValueChange = onHostVolumeChange,
                    valueRange = 0f..1f
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onTogglePlayback,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (state.playing) "Pause" else "Resume")
                    }
                    OutlinedButton(
                        onClick = onStop,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        androidx.compose.material3.Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Stop")
                    }
                }
            } else {
                Text(
                    "Playback is controlled by the host device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun SyncQueueHeader(count: Int) {
    SyncSectionHeading("Shared queue", "$count track${if (count == 1) "" else "s"}")
}

@Composable
private fun SyncQueueTrackCard(track: RexTrack, onPlay: () -> Unit) {
    Card(
        onClick = onPlay,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            androidx.compose.material3.Icon(Icons.Default.PlayArrow, contentDescription = "Putar dari antrean", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SyncOnlineSearchCard(
    query: String,
    onQueryChange: (String) -> Unit,
    searching: Boolean,
    searchMessage: String,
    onSearch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SyncSectionHeading("Find music", "Search online tracks to download for the shared room")
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Judul lagu atau artis") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !searching,
                trailingIcon = {
                    TextButton(
                        enabled = !searching && query.isNotBlank(),
                        onClick = onSearch
                    ) {
                        Text(if (searching) "…" else "CARI")
                    }
                }
            )
            if (searching) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Mencari lagu…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (searchMessage.isNotBlank()) {
                Text(
                    searchMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SyncOnlineResultCard(
    track: RexTrack,
    downloaded: Boolean,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            TextButton(onClick = onDownload) {
                Text(if (downloaded) "TERSIMPAN" else "UNDUH")
            }
        }
    }
}

@Composable
private fun SyncOfflineHeader(count: Int, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            SyncSectionHeading("Your offline library", "$count lagu tersedia di perangkat")
        }
        if (active) {
            Text("TAP + TO QUEUE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SyncOfflineTrackCard(
    track: RexTrack,
    active: Boolean,
    onQueue: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                androidx.compose.material3.Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            TextButton(enabled = active, onClick = onQueue) { Text("+ QUEUE") }
        }
    }
}

@Composable
private fun SyncEmptyQueueCard() {
    Text(
        "The shared queue is empty. Add a downloaded track from the offline library to get started.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

@Composable
private fun SyncEmptyOfflineCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Text(
            "No offline tracks yet. Search for music above and download it before adding it to a room.",
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SyncWaitingCard(connecting: Boolean, error: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (error != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (connecting) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
            } else {
                androidx.compose.material3.Icon(
                    Icons.Default.Groups,
                    contentDescription = null,
                    tint = if (error != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                if (connecting) "Connecting to room…" else if (error != null) "Connection needs attention" else "Not connected to a room",
                style = MaterialTheme.typography.titleMedium,
                color = if (error != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                if (connecting) "RexMusic is trying to establish the local Wi-Fi connection." else "Create a room on this device or enter a host IP to join an existing room.",
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SyncMembersEmptyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Connected members", style = MaterialTheme.typography.titleMedium)
            Text("Members will appear here after a room is active.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SyncPlaybackEmptyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Playback synchronization", style = MaterialTheme.typography.titleMedium)
            Text("The shared playback panel appears when you host or join a room.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
