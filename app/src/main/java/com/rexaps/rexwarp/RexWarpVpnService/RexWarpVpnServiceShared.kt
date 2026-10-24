package com.rexaps.rexwarp

import android.content.pm.ServiceInfo
import android.net.*
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.rexaps.rexwarp.tunnel.*
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first

internal fun RexWarpVpnService.__splitImpl_startConnect() {
        if (connectJob?.isActive == true || tunnel != null) return
        failureInFlight = false; stopping = false; userWantsConnection = true; paused = false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return handleFailure(RexWarpError.UNSUPPORTED_ANDROID)
        val provider = RexWarpTunnelRegistry.provider
        if (provider.unavailableReason != null) return handleFailure(RexWarpError.ENGINE_UNAVAILABLE)
        if (android.net.VpnService.prepare(this) != null) return handleFailure(RexWarpError.PERMISSION_REVOKED)
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
                withTimeout(RexWarpVpnService.CONNECT_TIMEOUT_MS) { t.connect(config) }
                if (!t.isConnected()) handleFailure(RexWarpError.TUNNEL_FAILED)
            } catch (e: TimeoutCancellationException) {
                handleFailure(RexWarpError.TIMEOUT)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(RexWarpVpnService.TAG, "connect failed: ${e.javaClass.simpleName}")
                // Ambil penyebab spesifik dari tunnel (mis. WARP_NOT_ACTIVE, REGISTRATION_FAILED).
                val specific = (tunnel?.state?.value as? RexWarpTunnelState.Failed)?.error
                handleFailure(specific ?: RexWarpError.TUNNEL_FAILED)
            }
        }
    }

internal fun RexWarpVpnService.__splitImpl_sample(t: RexWarpTunnel) {
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
        while (dlHist.size > RexWarpVpnService.HISTORY_SIZE) dlHist.removeFirst()
        while (ulHist.size > RexWarpVpnService.HISTORY_SIZE) ulHist.removeFirst()
        repo.updateLive(lastSpeed, s)
    }

/**
     * Android 14+: tipe SYSTEM_EXEMPTED bisa ditolak untuk app yang tidak memenuhi syarat.
     * Coba beberapa tipe berurutan supaya service tidak langsung mati.
     */
    internal fun RexWarpVpnService.__splitImpl_startForegroundSafely(): Boolean {
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
                Log.w(RexWarpVpnService.TAG, "startForeground(type=$type) failed", e)
            }
        }
        repo.setError(RexWarpError.SERVICE_STOPPED); stopSelf()
        return false
    }

internal fun RexWarpVpnService.__splitImpl_pauseReason(): String? {
        val caps = physical.values.firstOrNull { it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) }
            ?: physical.values.firstOrNull()
            ?: cm.activeNetwork?.let(cm::getNetworkCapabilities) ?: return null
        val s = settings
        return when {
            s.pauseOnWifi && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Paused on Wi-Fi"
            s.pauseOnMobile && caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Paused on mobile data"
            s.pauseOnMetered && !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> "Paused on metered network"
            else -> null
        }
    }
