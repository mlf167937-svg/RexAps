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
 *
 * - WARP mati:
 *   selisih TrafficStats
 *
 * - WARP aktif:
 *   selisih counter tunnel
 *
 * Saat service mulai:
 *
 *   CSV
 *     ↓
 *   AUTO RESTORE
 *     ↓
 *   Room
 *     ↓
 *   ambil counter terbaru
 *     ↓
 *   recording lanjut
 */
class RexWarpRecorderService : Service() {

    private val scope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Default
        )

    private var job: Job? = null

    private lateinit var repo: RexWarpRepository

    override fun onBind(
        intent: Intent?
    ): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        repo =
            RexWarpRepository.get(
                applicationContext
            )

        getSystemService(
            NotificationManager::class.java
        ).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "RexWARP usage recorder",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        /*
         * Stop manual dari notification.
         */
        if (intent?.action == ACTION_STOP) {

            RexWarpBackupStore
                .get(this)
                .recording = false

            stopForeground(
                STOP_FOREGROUND_REMOVE
            )

            stopSelf()

            return START_NOT_STICKY
        }

        /*
         * Foreground notification.
         */
        try {

            val notification =
                buildNotification()

            if (Build.VERSION.SDK_INT >= 34) {

                startForeground(
                    NOTIF_ID,
                    notification,
                    ServiceInfo
                        .FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )

            } else {

                startForeground(
                    NOTIF_ID,
                    notification
                )
            }

        } catch (e: Exception) {

            Log.w(
                TAG,
                "startForeground failed",
                e
            )

            stopSelf()

            return START_NOT_STICKY
        }

        /*
         * Jangan membuat dua recorder loop
         * kalau Android memanggil onStartCommand
         * lebih dari sekali.
         */
        if (job?.isActive != true) {

            job =
                scope.launch {
                    recordLoop()
                }
        }

        return START_STICKY
    }

    /**
     * Loop utama Usage Recorder.
     */
    private suspend fun recordLoop() {

        /*
         * ========================================================
         * 1. AUTO RESTORE CSV -> ROOM
         * ========================================================
         *
         * Harus dilakukan SEBELUM mengambil baseline
         * TrafficStats / tunnel counter.
         */
        runCatching {

            val restored =
                repo.usage.autoRestoreFromFolder()

            if (restored > 0) {

                Log.i(
                    TAG,
                    "Auto-restored $restored day(s) from CSV backup."
                )

            } else {

                Log.i(
                    TAG,
                    "No CSV backup found. Starting new recording."
                )
            }

        }.onFailure { error ->

            /*
             * CSV gagal dibaca bukan alasan
             * untuk mematikan Usage Recorder.
             *
             * Recorder tetap berjalan.
             */
            Log.w(
                TAG,
                "Auto-restore failed. Recording will continue.",
                error
            )
        }

        /*
         * ========================================================
         * 2. AMBIL BASELINE TRAFFIC COUNTER
         * ========================================================
         *
         * Penting:
         *
         * Restore harus selesai lebih dulu.
         *
         * Setelah itu counter sekarang dijadikan
         * baseline baru.
         *
         * Kita tidak mencoba menghitung trafik
         * selama service sedang mati.
         */
        var lastRx =
            TrafficStats.getTotalRxBytes()

        var lastTx =
            TrafficStats.getTotalTxBytes()

        /*
         * ========================================================
         * 3. BASELINE COUNTER WARP
         * ========================================================
         */
        var tunPrev:
            RexWarpTunnelStats? = null

        var tunSession:
            Long? = null

        repo.state.value.let { state ->

            if (
                state.connection ==
                    RexWarpConnectionState.CONNECTED &&
                state.stats != null
            ) {

                tunPrev =
                    state.stats

                tunSession =
                    state.sessionStartElapsedMs
            }
        }

        /*
         * ========================================================
         * 4. RECORDING LOOP
         * ========================================================
         */
        while (true) {

            /*
             * Tunggu sampai batas menit berikutnya.
             */
            delay(
                60_000L -
                    System.currentTimeMillis() %
                    60_000L
            )

            /*
             * Menit yang baru selesai.
             */
            val minute =
                System.currentTimeMillis() /
                    60_000L - 1

            /*
             * ====================================================
             * DEVICE TRAFFIC COUNTER
             * ====================================================
             */
            val rx =
                TrafficStats.getTotalRxBytes()

            val tx =
                TrafficStats.getTotalTxBytes()

            val devDl =
                if (
                    lastRx >= 0 &&
                    rx >= 0
                ) {

                    RexWarpFormat.delta(
                        lastRx,
                        rx
                    )

                } else {
                    0L
                }

            val devUl =
                if (
                    lastTx >= 0 &&
                    tx >= 0
                ) {

                    RexWarpFormat.delta(
                        lastTx,
                        tx
                    )

                } else {
                    0L
                }

            /*
             * Counter sekarang menjadi baseline
             * untuk menit berikutnya.
             */
            lastRx = rx
            lastTx = tx

            /*
             * ====================================================
             * CEK STATUS WARP
             * ====================================================
             */
            val state =
                repo.state.value

            val stats =
                state.stats

            val warp =
                state.connection ==
                    RexWarpConnectionState.CONNECTED &&
                    stats != null

            val dl: Long
            val ul: Long

            if (
                warp &&
                stats != null
            ) {

                /*
                 * =================================================
                 * WARP ACTIVE
                 * =================================================
                 *
                 * Kalau session berubah berarti tunnel
                 * baru saja reconnect.
                 *
                 * Counter tunnel dimulai lagi dari 0.
                 */
                if (
                    state.sessionStartElapsedMs !=
                    tunSession
                ) {

                    tunPrev =
                        RexWarpTunnelStats()

                    tunSession =
                        state.sessionStartElapsedMs
                }

                val prev =
                    tunPrev
                        ?: RexWarpTunnelStats()

                dl =
                    RexWarpFormat.delta(
                        prev.rxBytes,
                        stats.rxBytes
                    )

                ul =
                    RexWarpFormat.delta(
                        prev.txBytes,
                        stats.txBytes
                    )

                /*
                 * Counter terbaru menjadi baseline.
                 */
                tunPrev =
                    stats

            } else {

                /*
                 * =================================================
                 * WARP OFF
                 * =================================================
                 *
                 * Pakai TrafficStats perangkat.
                 */
                dl = devDl
                ul = devUl

                /*
                 * Reset baseline tunnel.
                 */
                tunPrev = null
                tunSession = null
            }

            /*
             * ====================================================
             * SIMPAN DATA
             * ====================================================
             *
             * record() melakukan:
             *
             * Room usage_minute
             * Room usage_daily
             * CSV backup
             */
            runCatching {

                repo.usage.record(
                    minute = minute,
                    dl = dl,
                    ul = ul,
                    warp = warp
                )

            }.onFailure { error ->

                Log.w(
                    TAG,
                    "record failed",
                    error
                )
            }

            /*
             * Bersihkan data per-menit yang lebih tua
             * dari 400 hari.
             *
             * Data daily tetap dipertahankan.
             */
            if (
                minute % 60L == 0L
            ) {

                runCatching {

                    repo.usage
                        .pruneMinutesOlderThan(
                            RETENTION_DAYS
                        )

                }.onFailure { error ->

                    Log.w(
                        TAG,
                        "prune failed",
                        error
                    )
                }
            }
        }
    }

    /**
     * Notification Usage Recorder.
     */
    private fun buildNotification(): Notification {

        val open =
            packageManager
                .getLaunchIntentForPackage(
                    packageName
                )
                ?.let { intent ->

                    PendingIntent.getActivity(
                        this,
                        2,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or
                            PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }

        val stop =
            PendingIntent.getService(
                this,
                3,
                Intent(
                    this,
                    RexWarpRecorderService::class.java
                ).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE
            )

        return NotificationCompat
            .Builder(
                this,
                CHANNEL_ID
            )
            .setSmallIcon(
                R.drawable.ic_rexwarp_notification
            )
            .setContentTitle(
                "RexWARP usage recorder"
            )
            .setContentText(
                "Recording network usage every minute"
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(
                0,
                "Stop",
                stop
            )
            .build()
    }

    override fun onDestroy() {

        scope.cancel()

        super.onDestroy()
    }

    companion object {

        const val ACTION_STOP =
            "com.rexaps.rexwarp.RECORDER_STOP"

        private const val CHANNEL_ID =
            "rexwarp_recorder"

        private const val NOTIF_ID =
            0x7258

        private const val TAG =
            "RexWarpRecorder"

        /*
         * Data per-menit disimpan 400 hari.
         * Total harian tetap disimpan.
         */
        private const val RETENTION_DAYS =
            400L

        fun start(
            ctx: Context
        ) {

            ContextCompat
                .startForegroundService(
                    ctx,
                    Intent(
                        ctx,
                        RexWarpRecorderService::class.java
                    )
                )
        }

        fun stop(
            ctx: Context
        ) {

            ctx.stopService(
                Intent(
                    ctx,
                    RexWarpRecorderService::class.java
                )
            )
        }
    }
}