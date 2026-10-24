package com.rexaps.rexmusic

import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

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

    fun detectIp(context: Context): String = detectIpImpl(context)
    fun createRoom(room: String, password: String, ip: String) = createRoomImpl(room, password, ip)
    fun joinRoom(room: String, name: String, ip: String, password: String) = joinRoomImpl(room, name, ip, password)
    internal var appContextRef: Context? = null
    fun bindContext(context: Context) = bindContextImpl(context)
    fun playTrack(track: RexTrack) = playTrackImpl(track)
    fun togglePlayback() = togglePlaybackImpl()
    fun stopAll(reason: String = "Host menghentikan pemutaran") = stopAllImpl(reason)
    fun addToQueue(track: RexTrack) = addToQueueImpl(track)
    fun playQueued(track: RexTrack) = playQueuedImpl(track)
    fun setHostVolume(value: Float) = setHostVolumeImpl(value)
    fun setMemberVolume(id: String, value: Float) = setMemberVolumeImpl(id, value)
    fun disconnect() { closeAll(); _state.value = _state.value.copy(hosting = false, connected = false, playing = false, members = emptyList(), status = "Terputus", error = null) }
}
