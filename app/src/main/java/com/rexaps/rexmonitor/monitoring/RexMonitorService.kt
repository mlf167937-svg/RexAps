package com.rexaps.rexmonitor.monitoring

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

enum class RexServiceStatus { STOPPED, STARTING, RUNNING, FAILED }

/**
 * Foreground service. Hanya hidup saat Monitoring Notification ON.
 * START_NOT_STICKY: bila proses mati, status kembali OFF (jujur), bukan diam-diam hidup lagi.
 */
class RexMonitorService : Service() {
    private var scope: CoroutineScope? = null
    private var job: Job? = null
    private lateinit var notifier: RexMonitorNotification
    private lateinit var repository: RexMonitorRepository

    override fun onCreate() {
        super.onCreate()
        notifier = RexMonitorNotification(this)
        notifier.ensureChannel()
        repository = RexMonitorRepository(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopMonitoring()
            return START_NOT_STICKY
        }
        if (job?.isActive == true) return START_NOT_STICKY

        try {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
            ServiceCompat.startForeground(
                this, RexMonitorNotification.NOTIFICATION_ID, notifier.build(null), type
            )
        } catch (_: Throwable) {
            _status.value = RexServiceStatus.FAILED
            stopSelf()
            return START_NOT_STICKY
        }

        _status.value = RexServiceStatus.RUNNING
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = s
        job = s.launch {
            repository.stream(INTERVAL_MS)
                .catch { _status.value = RexServiceStatus.FAILED }
                .collect { notifier.update(it) }
        }
        return START_NOT_STICKY
    }

    private fun stopMonitoring() {
        job?.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope?.cancel()
        scope = null
        job = null
        repository.close()
        if (_status.value != RexServiceStatus.FAILED) _status.value = RexServiceStatus.STOPPED
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.rexaps.rexmonitor.action.START"
        const val ACTION_STOP = "com.rexaps.rexmonitor.action.STOP"
        private const val INTERVAL_MS = 4_000L

        private val _status = MutableStateFlow(RexServiceStatus.STOPPED)
        val status: StateFlow<RexServiceStatus> = _status.asStateFlow()

        fun startIntent(context: Context): Intent =
            Intent(context, RexMonitorService::class.java).setAction(ACTION_START)

        fun markStarting() { _status.value = RexServiceStatus.STARTING }
        fun markFailed() { _status.value = RexServiceStatus.FAILED }
    }
}
