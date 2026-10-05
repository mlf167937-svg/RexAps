package com.rexaps.rexwarp

import android.content.Context
import com.rexaps.rexwarp.data.RexWarpDatabase
import com.rexaps.rexwarp.model.RexWarpConnectionInfo
import com.rexaps.rexwarp.tunnel.RexWarpTunnelRegistry
import com.rexaps.rexwarp.tunnel.RexWarpTunnelStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Sumber kebenaran tunggal antara service dan UI (satu proses). */
class RexWarpRepository private constructor(context: Context) {

    init { RexWarpTunnelRegistry.installDefault(context) } // harus sebelum state dibuat

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val preferences = RexWarpPreferences(context)
    val usage = RexWarpUsageRepository(
        RexWarpDatabase.get(context).usageDao(),
        RexWarpBackupStore.get(context)
    )

    private val _state = MutableStateFlow(
        RexWarpState(engineUnavailableReason = RexWarpTunnelRegistry.provider.unavailableReason)
    )
    val state: StateFlow<RexWarpState> = _state.asStateFlow()

    fun setConnecting() = _state.update { it.copy(connection = RexWarpConnectionState.CONNECTING, error = null, pausedReason = null) }
    fun setConnected(startElapsed: Long) = _state.update {
        it.copy(connection = RexWarpConnectionState.CONNECTED, error = null, pausedReason = null, sessionStartElapsedMs = startElapsed)
    }
    fun setDisconnecting() = _state.update { it.copy(connection = RexWarpConnectionState.DISCONNECTING) }
    fun setDisconnected(pausedReason: String? = null) = _state.update { clear(it).copy(connection = RexWarpConnectionState.DISCONNECTED, pausedReason = pausedReason) }
    fun setError(e: RexWarpError) = _state.update { clear(it).copy(connection = RexWarpConnectionState.ERROR, error = e) }
    fun updateInfo(i: RexWarpConnectionInfo) = _state.update { it.copy(info = i) }
    fun updateLive(speed: RexWarpSpeed, stats: RexWarpTunnelStats) = _state.update { it.copy(speed = speed, stats = stats) }

    private fun clear(s: RexWarpState) = s.copy(
        error = null, pausedReason = null, info = RexWarpConnectionInfo(), speed = RexWarpSpeed(),
        stats = null, sessionStartElapsedMs = null,
        engineUnavailableReason = RexWarpTunnelRegistry.provider.unavailableReason
    )

    companion object {
        @Volatile private var instance: RexWarpRepository? = null
        fun get(context: Context): RexWarpRepository = instance ?: synchronized(this) {
            instance ?: RexWarpRepository(context.applicationContext).also { instance = it }
        }
    }
}
