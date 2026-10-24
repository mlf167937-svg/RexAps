package com.rexaps.rexmusic

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*

internal const val SYNC_PORT = 38741

internal const val TRANSFER_PORT = 38742

internal const val HEARTBEAT_MS = 800L

internal const val CLIENT_TIMEOUT_MS = 3500L

internal const val START_LEAD_MS = 3000L

internal const val DRIFT_CORRECTION_MS = 250L

internal data class SyncMember(val id: String, val name: String, val connectedAt: Long = SystemClock.elapsedRealtime(), val ip: String = "", val volume: Float = 1f, val deviceVolume: Float = -1f)

internal data class SyncUiState(
    val hosting: Boolean = false, val connected: Boolean = false, val room: String = "RexSyncRoom1",
    val ip: String = "", val password: String = "rexsync", val name: String = "User",
    val status: String = "Belum terhubung", val members: List<SyncMember> = emptyList(),
    val trackId: String = "", val trackTitle: String = "Belum ada lagu", val positionMs: Long = 0L,
    val playing: Boolean = false, val error: String? = null, val queue: List<RexTrack> = emptyList(), val cacheStatus: String = "", val volumes: Map<String, Float> = emptyMap(), val hostVolume: Float = 1f, val roomElapsedMs: Long = 0L
)

internal fun formatRoomTime(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) / 1000L)
    return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", total / 3600L, (total / 60L) % 60L, total % 60L)
}
