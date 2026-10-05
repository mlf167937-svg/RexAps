package com.rexaps.rexwarp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.TrafficStats
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.rexaps.R
import com.rexaps.rexwarp.tunnel.RexWarpTunnelStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Mencatat trafik tiap menit, termasuk saat WARP mati.
 * - WARP mati: selisih TrafficStats (seluruh trafik perangkat).
 * - WARP aktif: selisih counter tunnel (menghindari hitungan ganda tun0 + jaringan fisik).
 */
class RexWarpRecorderService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private lateinit var repo: RexWarpRepository

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repo = RexWarpRepository.get(applicationContext)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "RexWARP usage recorder", NotificationManager.IMPORTANCE_LOW)
                .apply { setShowBadge(false) }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            RexWarpBackupStore.get(this).recording = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            val n = buildNotification()
            if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else startForeground(NOTIF_ID, n)
        } catch (e: Exception) {
            Log.w(TAG, "startForeground failed", e)
            stopSelf()
            return START_NOT_STICKY
        }
        if (job?.isActive != true) job = scope.launch { recordLoop() }
        return START_STICKY
    }

    private suspend fun recordLoop() {
        var lastRx = TrafficStats.getTotalRxBytes()
        var lastTx = TrafficStats.getTotalTxBytes()
        var tunPrev: RexWarpTunnelStats? = null
        var tunSession: Long? = null
        repo.state.value.let { st ->
            if (st.connection == RexWarpConnectionState.CONNECTED && st.stats != null) {
                tunPrev = st.stats; tunSession = st.sessionStartElapsedMs
            }
        }

        while (true) {
            delay(60_000L - System.currentTimeMillis() % 60_000L) // tunggu batas menit berikutnya
            val minute = System.currentTimeMillis() / 60_000L - 1   // trafik dicatat ke menit yang baru selesai

            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            val devDl = if (lastRx >= 0 && rx >= 0) RexWarpFormat.delta(lastRx, rx) else 0L
            val devUl = if (lastTx >= 0 && tx >= 0) RexWarpFormat.delta(lastTx, tx) else 0L
            lastRx = rx; lastTx = tx

            val st = repo.state.value
            val stats = st.stats
            val warp = st.connection == RexWarpConnectionState.CONNECTED && stats != null
            val dl: Long; val ul: Long
            if (warp && stats != null) {
                if (st.sessionStartElapsedMs != tunSession) { // sesi baru: counter mulai dari 0
                    tunPrev = RexWarpTunnelStats(); tunSession = st.sessionStartElapsedMs
                }
                val prev = tunPrev ?: RexWarpTunnelStats()
                dl = RexWarpFormat.delta(prev.rxBytes, stats.rxBytes)
                ul = RexWarpFormat.delta(prev.txBytes, stats.txBytes)
                tunPrev = stats
            } else {
                dl = devDl; ul = devUl
                tunPrev = null; tunSession = null
            }

            runCatching { repo.usage.record(minute, dl, ul, warp) }.onFailure { Log.w(TAG, "record failed", it) }
            if (minute % 60L == 0L) runCatching { repo.usage.pruneMinutesOlderThan(RETENTION_DAYS) }
        }
    }

    private fun buildNotification(): Notification {
        val open = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 2, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val stop = PendingIntent.getService(
            this, 3, Intent(this, RexWarpRecorderService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_rexwarp_notification)
            .setContentTitle("RexWARP usage recorder")
            .setContentText("Recording network usage every minute")
            .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(open)
            .addAction(0, "Stop", stop)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.rexaps.rexwarp.RECORDER_STOP"
        private const val CHANNEL_ID = "rexwarp_recorder"
        private const val NOTIF_ID = 0x7258
        private const val TAG = "RexWarpRecorder"
        private const val RETENTION_DAYS = 400L // data per menit; total harian disimpan selamanya

        fun start(ctx: Context) = ContextCompat.startForegroundService(ctx, Intent(ctx, RexWarpRecorderService::class.java))
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, RexWarpRecorderService::class.java)) }
    }
}