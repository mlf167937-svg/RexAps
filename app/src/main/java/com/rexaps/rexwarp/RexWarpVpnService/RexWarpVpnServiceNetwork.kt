package com.rexaps.rexwarp

import android.net.*
import android.os.SystemClock
import com.rexaps.rexwarp.tunnel.*
import kotlinx.coroutines.*

internal fun RexWarpVpnService.__split_onTunnelState_5(s: RexWarpTunnelState) {
        if (stopping) return
        when (s) {
            RexWarpTunnelState.Connecting -> repo.setConnecting()
            RexWarpTunnelState.Connected -> {
                val t = tunnel ?: return
                if (!t.isConnected()) return // jangan pernah tampilkan CONNECTED tanpa tunnel aktif
                sessionActive = true; reconnectAttempts = 0
                val start = SystemClock.elapsedRealtime()
                repo.setConnected(start); repo.updateInfo(t.getConnectionInfo())
                startMonitor(t); updateNotification()
            }
            RexWarpTunnelState.Disconnecting -> repo.setDisconnecting()
            RexWarpTunnelState.Disconnected -> if (sessionActive && userWantsConnection && !paused) handleFailure(RexWarpError.UNEXPECTED_DISCONNECT)
            is RexWarpTunnelState.Failed -> handleFailure(s.error)
        }
    }

internal suspend fun RexWarpVpnService.__split_teardownTunnel_6() {
        val t = tunnel ?: return
        tunnel = null; sessionActive = false
        runCatching { sample(t) }; flushPending()
        lastSpeed = RexWarpSpeed(); dlHist.clear(); ulHist.clear()
        monitorJob?.cancel(); stateJob?.cancel(); infoJob?.cancel(); connectJob?.cancel()
        runCatching { withTimeout(DISCONNECT_TIMEOUT_MS) { t.disconnect() } }
    }
