package com.rexaps.rexwarp

import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.*
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.rexaps.rexwarp.tunnel.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import java.time.LocalDate

internal fun RexWarpVpnService.__split_onStartCommand_1(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                if (!startForegroundSafely()) return START_NOT_STICKY
                startConnect()
            }
            ACTION_DISCONNECT -> userDisconnect()
            else -> if (tunnel == null && !paused) stopSelf()
        }
        return START_NOT_STICKY
    }

internal fun RexWarpVpnService.__split_startConnect_2() {
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

internal fun RexWarpVpnService.__split_userDisconnect_3() {
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

internal fun RexWarpVpnService.__split_handleFailure_4(error: RexWarpError) {
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

internal fun RexWarpVpnService.__split_finishService_7() {
        // Notifikasi error tetap terlihat setelah service berhenti (jangan dihapus bila ERROR).
        stopForeground(if (repo.state.value.connection == RexWarpConnectionState.ERROR) STOP_FOREGROUND_DETACH else STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

internal fun RexWarpVpnService.__split_startMonitor_8(t: RexWarpTunnel) {
        monitorJob?.cancel()
        val s0 = t.getStats()
        lastRx = s0.rxBytes; lastTx = s0.txBytes; lastSampleAt = SystemClock.elapsedRealtime()
        monitorJob = scope.launch {
            var tick = 0
            while (isActive) {
                delay(SAMPLE_MS)
                tick++
                sample(t)
                updateNotification(force = false)         // real-time tiap detik (dilewati saat layar mati)
                if (tick % 15 == 0) flushPending()        // tulis DB tiap ~15 detik
            }
        }
    }

internal fun RexWarpVpnService.__split_sample_9(t: RexWarpTunnel) {
        val now = SystemClock.elapsedRealtime()
        val s = t.getStats()
        val dRx = RexWarpFormat.delta(lastRx, s.rxBytes); val dTx = RexWarpFormat.delta(lastTx, s.txBytes)
        val dt = (now - lastSampleAt).coerceAtLeast(1)
        lastRx = s.rxBytes; lastTx = s.txBytes; lastSampleAt = now
        val today = LocalDate.now()
        if (today != pendingDate) { flushPending(); pendingDate = today }
        pendingDl += dRx; pendingUl += dTx
        lastSpeed = RexWarpSpeed(dRx * 1000 / dt, dTx * 1000 / dt)
        dlHist.addLast(lastSpeed.downBytesPerSec); ulHist.addLast(lastSpeed.upBytesPerSec)
        while (dlHist.size > HISTORY_SIZE) dlHist.removeFirst()
        while (ulHist.size > HISTORY_SIZE) ulHist.removeFirst()
        repo.updateLive(lastSpeed, s)
    }

internal fun RexWarpVpnService.__split_flushPending_10() {
        if (pendingDl == 0L && pendingUl == 0L) return
        val d = pendingDate; val dl = pendingDl; val ul = pendingUl
        pendingDl = 0; pendingUl = 0
        repo.appScope.launch { repo.usage.addTraffic(d, dl, ul) } // tetap jalan walau service sudah mati
    }

internal fun RexWarpVpnService.__split_evaluatePolicy_12() {
        val reason = pauseReason()
        if (reason != null && userWantsConnection && !paused && (tunnel != null || connectJob?.isActive == true)) {
            paused = true; stopping = true; connectJob?.cancel()
            scope.launch { teardownTunnel(); stopping = false; repo.setDisconnected(reason); updateNotification() }
        } else if (reason == null && paused && userWantsConnection && physical.isNotEmpty()) {
            paused = false; startConnect()
        }
    }

/**
     * Android 14+: tipe SYSTEM_EXEMPTED bisa ditolak untuk app yang tidak memenuhi syarat.
     * Coba beberapa tipe berurutan supaya service tidak langsung mati.
     */
    internal fun RexWarpVpnService.__split_startForegroundSafely_13(): Boolean {
        val n = RexWarpNotification.build(this, RexWarpConnectionState.CONNECTING, null, null, null)
        val types: List<Int> = if (Build.VERSION.SDK_INT >= 34)
            listOf(ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else listOf(0)
        for (type in types) {
            try {
                if (Build.VERSION.SDK_INT >= 34) startForeground(RexWarpNotification.ID, n, type)
                else startForeground(RexWarpNotification.ID, n)
                return true
            } catch (e: Exception) {
                Log.w(TAG, "startForeground(type=$type) failed", e)
            }
        }
        repo.setError(RexWarpError.SERVICE_STOPPED); stopSelf()
        return false
    }

/** [force] = false: lewati bila layar mati (hemat baterai); notifikasi segar lagi saat layar menyala. */
    internal fun RexWarpVpnService.__split_updateNotification_14(force: Boolean = true) {
        if (!force && getSystemService(PowerManager::class.java)?.isInteractive == false) return
        val st = repo.state.value
        val connected = st.connection == RexWarpConnectionState.CONNECTED
        val wall = st.sessionStartElapsedMs?.let { System.currentTimeMillis() - (SystemClock.elapsedRealtime() - it) }
        getSystemService(NotificationManager::class.java).notify(
            RexWarpNotification.ID,
            RexWarpNotification.build(
                this, st.connection,
                if (connected) lastSpeed else null,
                wall, st.pausedReason, st.stats, st.info,
                if (connected) RexWarpSpeedHistory(dlHist.toList(), ulHist.toList()) else null
            )
        )
    }
