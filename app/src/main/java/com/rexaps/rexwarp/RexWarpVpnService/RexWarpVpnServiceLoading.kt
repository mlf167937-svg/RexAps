package com.rexaps.rexwarp

import android.net.*
import android.util.Log
import com.rexaps.rexwarp.tunnel.*
import kotlinx.coroutines.*

internal fun RexWarpVpnService.__splitImpl_handleFailure(error: RexWarpError) {
        if (stopping || failureInFlight) return
        failureInFlight = true
        Log.d(RexWarpVpnService.TAG, "failure: $error")
        val retry = userWantsConnection && settings.autoConnect && error in RexWarpVpnService.RETRYABLE && reconnectAttempts < RexWarpVpnService.MAX_RECONNECT
        scope.launch {
            teardownTunnel()
            if (retry) {
                reconnectAttempts++
                repo.setConnecting(); updateNotification()
                reconnectJob = launch { delay(RexWarpVpnService.RECONNECT_BASE_DELAY_MS * reconnectAttempts); startConnect() }
            } else {
                userWantsConnection = false
                repo.setError(error)
                updateNotification()
                finishService()
            }
        }
    }
