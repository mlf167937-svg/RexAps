package com.rexaps.rexwarp

import android.net.*
import android.util.Log
import com.rexaps.rexwarp.tunnel.*
import kotlinx.coroutines.*

internal fun RexWarpVpnService.__splitImpl_handleFailure(error: RexWarpError) {
        if (stopping || failureInFlight) return
        failureInFlight = true
        Log.d(TAG, "failure: $error")
        val retry = userWantsConnection && settings.autoConnect && error in RETRYABLE && reconnectAttempts < MAX_RECONNECT
        scope.launch {
            teardownTunnel()
            if (retry) {
                reconnectAttempts++
                repo.setConnecting(); updateNotification()
                reconnectJob = launch { delay(RECONNECT_BASE_DELAY_MS * reconnectAttempts); startConnect() }
            } else {
                userWantsConnection = false
                repo.setError(error)
                updateNotification()
                finishService()
            }
        }
    }
