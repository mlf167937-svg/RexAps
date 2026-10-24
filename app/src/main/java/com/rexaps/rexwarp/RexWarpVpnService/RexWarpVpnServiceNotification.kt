package com.rexaps.rexwarp

import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.net.*
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.rexaps.rexwarp.tunnel.*
import kotlinx.coroutines.*

// ---- notifikasi / lifecycle ----------------------------------------------------------

/**
 * Android 14+: tipe SYSTEM_EXEMPTED bisa ditolak untuk app yang tidak memenuhi syarat.
 * Coba beberapa tipe berurutan supaya service tidak langsung mati.
 */
internal fun RexWarpVpnService.startForegroundSafely(): Boolean {
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
internal fun RexWarpVpnService.updateNotification(force: Boolean = true) {
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
