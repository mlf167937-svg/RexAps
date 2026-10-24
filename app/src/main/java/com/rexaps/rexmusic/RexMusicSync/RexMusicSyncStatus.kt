package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*

internal data class SyncUiState(
    val hosting: Boolean = false, val connected: Boolean = false, val room: String = "RexSyncRoom1",
    val ip: String = "", val password: String = "rexsync", val name: String = "User",
    val status: String = "Belum terhubung", val members: List<SyncMember> = emptyList(),
    val trackId: String = "", val trackTitle: String = "Belum ada lagu", val positionMs: Long = 0L,
    val playing: Boolean = false, val error: String? = null, val queue: List<RexTrack> = emptyList(), val cacheStatus: String = "", val volumes: Map<String, Float> = emptyMap(), val hostVolume: Float = 1f, val roomElapsedMs: Long = 0L
)
