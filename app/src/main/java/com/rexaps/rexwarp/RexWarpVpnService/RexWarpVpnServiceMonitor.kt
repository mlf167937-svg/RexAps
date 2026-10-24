package com.rexaps.rexwarp

import android.net.*
import android.os.SystemClock
import com.rexaps.rexwarp.tunnel.*
import kotlinx.coroutines.*
import java.time.LocalDate

// ---- statistik & usage ---------------------------------------------------------------

internal fun RexWarpVpnService.startMonitor(t: RexWarpTunnel) {
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

internal fun RexWarpVpnService.sample(t: RexWarpTunnel) {
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

internal fun RexWarpVpnService.flushPending() {
    if (pendingDl == 0L && pendingUl == 0L) return
    val d = pendingDate; val dl = pendingDl; val ul = pendingUl
    pendingDl = 0; pendingUl = 0
    repo.appScope.launch { repo.usage.addTraffic(d, dl, ul) } // tetap jalan walau service sudah mati
}

// ---- aturan jaringan -----------------------------------------------------------------

internal fun RexWarpVpnService.hasNetwork(): Boolean {
    val n = cm.activeNetwork ?: return false
    return cm.getNetworkCapabilities(n)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}

internal fun RexWarpVpnService.pauseReason(): String? {
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

internal fun RexWarpVpnService.evaluatePolicy() {
    val reason = pauseReason()
    if (reason != null && userWantsConnection && !paused && (tunnel != null || connectJob?.isActive == true)) {
        paused = true; stopping = true; connectJob?.cancel()
        scope.launch { teardownTunnel(); stopping = false; repo.setDisconnected(reason); updateNotification() }
    } else if (reason == null && paused && userWantsConnection && physical.isNotEmpty()) {
        paused = false; startConnect()
    }
}
