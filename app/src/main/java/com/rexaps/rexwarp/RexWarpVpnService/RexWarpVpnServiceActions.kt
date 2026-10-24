package com.rexaps.rexwarp

import android.app.NotificationManager
import android.net.*
import android.os.PowerManager
import android.os.SystemClock
import com.rexaps.rexwarp.tunnel.*
import kotlinx.coroutines.*

/** [force] = false: lewati bila layar mati (hemat baterai); notifikasi segar lagi saat layar menyala. */
    internal fun RexWarpVpnService.__splitImpl_updateNotification(force: Boolean = true) {
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
