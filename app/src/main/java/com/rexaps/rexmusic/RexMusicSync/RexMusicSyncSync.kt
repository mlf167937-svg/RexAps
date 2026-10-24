package com.rexaps.rexmusic

import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

internal const val SYNC_PORT = 38741

internal data class SyncMember(val id: String, val name: String, val connectedAt: Long = SystemClock.elapsedRealtime(), val ip: String = "", val volume: Float = 1f, val deviceVolume: Float = -1f)

/** Local Wi-Fi sync protocol. Uses newline-delimited JSON control messages; no SSH or internet. */
internal object RexMusicSyncEngine {
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, error ->
        _state.value = _state.value.copy(error = "RexMusicSync: ${error.message ?: "terjadi kesalahan"}")
    })
    private val gson = Gson()
    internal val _state = MutableStateFlow(SyncUiState())
    val state = _state.asStateFlow()
    internal var server: ServerSocket? = null
    internal var transferServer: ServerSocket? = null
    internal var transferJob: Job? = null
    internal val sharedQueue = mutableListOf<RexTrack>()
    internal var roomJob: Job? = null
    internal val peers = CopyOnWriteArrayList<Peer>()
    internal var client: Socket? = null
    internal var clientWriter: BufferedWriter? = null
    internal var heartbeat: Job? = null
    internal var playbackJob: Job? = null
    internal var roomPassword = "rexsync"
    internal var roomName = "RexSyncRoom1"
    internal var hostTrack: RexTrack? = null
    internal var playAnchorWallMs = 0L // host monotonic elapsedRealtime timestamp
    internal var roomStartedElapsedMs = 0L
    @Volatile internal var clockOffsetMs = 0L // host elapsedRealtime - client elapsedRealtime
    @Volatile internal var lastClockRttMs = -1L
    internal var playAnchorPositionMs = 0L
    internal var hostPlaying = false
    internal var hostPreparing = false
    internal var resumeWhenMemberReturns = false

    internal data class Peer(val socket: Socket, val reader: BufferedReader, val writer: BufferedWriter,
                            var id: String = "", var name: String = "", @Volatile var lastSeen: Long = SystemClock.elapsedRealtime(), val ip: String = socket.inetAddress?.hostAddress.orEmpty(), @Volatile var volume: Float = 1f, @Volatile var deviceVolume: Float = -1f, @Volatile var clockSynced: Boolean = false)

    fun detectIp(context: Context): String = this.__split_detectIp_1(context)

    fun createRoom(room: String, password: String, ip: String) = this.__split_createRoom_2(room, password, ip)

    internal suspend fun acceptPeer(socket: Socket) = this.__split_acceptPeer_3(socket)

    internal suspend fun monitorPeers() = this.__split_monitorPeers_4()

    fun joinRoom(room: String, name: String, ip: String, password: String) = this.__split_joinRoom_5(room, name, ip, password)

    internal fun handleClientMessage(m: JSONObject) = this.__split_handleClientMessage_6(m)

    internal suspend fun syncLocalTrack(id: String, pos: Long, playing: Boolean, startAt: Long) = this.__split_syncLocalTrack_7(id, pos, playing, startAt)

    internal suspend fun reconcileLocalTrack(id: String, targetPosition: Long, shouldPlay: Boolean) = this.__split_reconcileLocalTrack_8(id, targetPosition, shouldPlay)

    internal var appContextRef: Context? = null
    fun bindContext(context: Context) = this.__split_bindContext_9(context)

    fun playTrack(track: RexTrack) = this.__split_playTrack_10(track)

    private fun resumeRoomAtCurrentPosition() = this.__split_resumeRoomAtCurrentPosition_11()

    fun togglePlayback() = this.__split_togglePlayback_12()

    fun stopAll(reason: String = "Host menghentikan pemutaran") = this.__split_stopAll_13(reason)

    internal fun sendState(p: Peer) = this.__split_sendState_14(p)
    internal fun broadcastState() { peers.filter { it.clockSynced }.forEach { sendState(it) } }
    internal fun broadcastTick() = this.__split_broadcastTick_15()
    internal fun broadcastMembers() = this.__split_broadcastMembers_16()
    fun addToQueue(track: RexTrack) = this.__split_addToQueue_17(track)

    fun playQueued(track: RexTrack) = this.__split_playQueued_18(track)

    fun setHostVolume(value: Float) = this.__split_setHostVolume_19(value)

    fun setMemberVolume(id: String, value: Float) = this.__split_setMemberVolume_20(id, value)

    internal fun trackJson(t: RexTrack) = this.__split_trackJson_21(t)
    internal fun trackFromJson(o: JSONObject?): RexTrack? = this.__split_trackFromJson_22(o)
    internal fun sendQueue(p: Peer) { val arr = org.json.JSONArray(); sharedQueue.forEach { arr.put(trackJson(it)) }; send(p.writer, JSONObject().put("type", "queue").put("items", arr)) }
    internal fun publishQueue() = this.__split_publishQueue_23()

    internal suspend fun distributeLocalTrack(track: RexTrack) = this.__split_distributeLocalTrack_24(track)

    internal suspend fun sendBundle(host: String, track: RexTrack): Boolean = this.__split_sendBundle_25(host, track)

    internal fun receiveBundle(socket: Socket) = this.__split_receiveBundle_26(socket)

    internal fun send(w: BufferedWriter, m: JSONObject) = this.__split_send_27(w, m)
    internal suspend fun withContextMain(block: () -> Unit) { withContext(Dispatchers.Main) { block() } }

    fun disconnect() { closeAll(); _state.value = _state.value.copy(hosting = false, connected = false, playing = false, members = emptyList(), status = "Terputus", error = null) }
    internal fun closeAll() = this.__split_closeAll_28()
    internal fun stopLocalPlayback() { appContextRef?.let { c -> scope.launch(Dispatchers.Main) { runCatching { RexPlayerController.init(c); RexPlayerController.pause() } } } }
}

internal fun formatRoomTime(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) / 1000L)
    return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", total / 3600L, (total / 60L) % 60L, total % 60L)
}
