package com.rexaps.rexwarp

import android.net.*
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.rexaps.rexwarp.tunnel.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first

// ---- connect / disconnect ------------------------------------------------------------

internal fun RexWarpVpnService.startConnect() {
    if (connectJob?.isActive == true || tunnel != null) return
    failureInFlight = false; stopping = false; userWantsConnection = true; paused = false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return handleFailure(RexWarpError.UNSUPPORTED_ANDROID)
    val provider = RexWarpTunnelRegistry.provider
    if (provider.unavailableReason != null) return handleFailure(RexWarpError.ENGINE_UNAVAILABLE)
    if (prepare(this) != null) return handleFailure(RexWarpError.PERMISSION_REVOKED)
    pauseReason()?.let { paused = true; repo.setDisconnected(it); updateNotification(); return }
    if (!hasNetwork()) return handleFailure(RexWarpError.NETWORK_UNAVAILABLE)

    repo.setConnecting(); updateNotification()
    connectJob = scope.launch {
        try {
            settings = repo.preferences.settings.first()
            val config = settings.toTunnelConfig() ?: return@launch handleFailure(RexWarpError.INVALID_CONFIG)
            val t = provider.create(Host())
            tunnel = t; sessionActive = false
            stateJob = scope.launch { t.state.collect(::onTunnelState) }
            infoJob = scope.launch {
                t.connectionInfo.collect { if (repo.state.value.connection == RexWarpConnectionState.CONNECTED) repo.updateInfo(it) }
            }
            withTimeout(CONNECT_TIMEOUT_MS) { t.connect(config) }
            if (!t.isConnected()) handleFailure(RexWarpError.TUNNEL_FAILED)
        } catch (e: TimeoutCancellationException) {
            handleFailure(RexWarpError.TIMEOUT)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "connect failed: ${e.javaClass.simpleName}")
            // Ambil penyebab spesifik dari tunnel (mis. WARP_NOT_ACTIVE, REGISTRATION_FAILED).
            val specific = (tunnel?.state?.value as? RexWarpTunnelState.Failed)?.error
            handleFailure(specific ?: RexWarpError.TUNNEL_FAILED)
        }
    }
}

internal fun RexWarpVpnService.userDisconnect() {
    userWantsConnection = false; paused = false; stopping = true
    reconnectJob?.cancel(); connectJob?.cancel()
    scope.launch {
        if (tunnel != null) repo.setDisconnecting()
        teardownTunnel()
        stopping = false
        repo.setDisconnected()
        finishService()
    }
}

internal fun RexWarpVpnService.handleFailure(error: RexWarpError) {
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

private fun RexWarpVpnService.onTunnelState(s: RexWarpTunnelState) {
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

internal suspend fun RexWarpVpnService.teardownTunnel() {
    val t = tunnel ?: return
    tunnel = null; sessionActive = false
    runCatching { sample(t) }; flushPending()
    lastSpeed = RexWarpSpeed(); dlHist.clear(); ulHist.clear()
    monitorJob?.cancel(); stateJob?.cancel(); infoJob?.cancel(); connectJob?.cancel()
    runCatching { withTimeout(DISCONNECT_TIMEOUT_MS) { t.disconnect() } }
}

private fun RexWarpVpnService.finishService() {
    // Notifikasi error tetap terlihat setelah service berhenti (jangan dihapus bila ERROR).
    stopForeground(if (repo.state.value.connection == RexWarpConnectionState.ERROR) STOP_FOREGROUND_DETACH else STOP_FOREGROUND_REMOVE)
    stopSelf()
}
