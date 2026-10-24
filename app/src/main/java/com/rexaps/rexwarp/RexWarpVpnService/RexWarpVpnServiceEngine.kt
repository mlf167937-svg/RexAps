package com.rexaps.rexwarp

import android.net.*
import android.os.SystemClock
import com.rexaps.rexwarp.tunnel.*
import kotlinx.coroutines.*

internal fun RexWarpVpnService.__splitImpl_onTunnelState(s: RexWarpTunnelState) {
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
