package com.rexaps.rexmusic

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.net.InetAddress
import java.net.ServerSocket
import kotlinx.coroutines.*

internal fun RexMusicSyncEngine.__splitImpl_createRoom(room: String, password: String, ip: String) {
        closeAll()
        roomName = room.trim().ifBlank { "RexSyncRoom1" }
        roomStartedElapsedMs = SystemClock.elapsedRealtime()
        roomPassword = password
        RexPlayerController.setSynchronizedPlaybackMode(true)
        autoRefreshJob?.cancel()
        val hostProfile = RexLocalProfileBridge.read()
        val hostName = hostProfile.name.trim().ifBlank { "Rex User" }
        val initialVolume = appContextRef?.let { readAndroidMusicVolume(it) }?.takeIf { it >= 0f } ?: 1f
        appContextRef?.let { context ->
            RexPlayerController.init(context)
            RexPlayerController.setSyncVolume(1f)
        }
        _state.value = _state.value.copy(
            hosting = true, connected = true, room = roomName, password = password,
            ip = ip, name = hostName, status = "Membuka room…", error = null,
            hostVolume = initialVolume, autoRefreshEnabled = false, resyncing = false,
            members = listOf(SyncMember("host", hostName, username = hostProfile.username, bio = hostProfile.bio))
        )
        roomJob = scope.launch {
            try {
                val assets = appContextRef?.let { withContext(Dispatchers.IO) { createRexSyncProfileAssets(it, hostProfile) } } ?: RexSyncProfileAssets()
                _state.value = _state.value.copy(members = listOf(SyncMember(
                    id = "host", name = hostName, username = hostProfile.username, bio = hostProfile.bio,
                    profilePictureBase64 = assets.pictureBase64, bannerBase64 = assets.bannerBase64
                )))
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
