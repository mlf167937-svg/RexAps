package com.rexaps.rexmonitor

import android.app.Application
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rexaps.rexmonitor.booster.RexBooster
import com.rexaps.rexmonitor.monitoring.RexMonitorRepository
import com.rexaps.rexmonitor.monitoring.RexMonitorService
import com.rexaps.rexmonitor.monitoring.RexServiceStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RexMonitorViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val repository = RexMonitorRepository(app)
    private val booster = RexBooster(app)

    private data class Local(
        val notificationPermission: RexNotificationPermission = RexNotificationPermission.UNKNOWN,
        val dialog: RexPermissionDialog = RexPermissionDialog.NONE,
        val rootAvailable: Boolean = false,
        val rootGranted: Boolean = false,
        val booster: RexBoosterState = RexBoosterState()
    )

    private val local = MutableStateFlow(Local())

    /**
     * Polling hanya berjalan selama UI mengoleksi (lifecycle-aware) dan berhenti 5 dtk
     * setelah UI tidak terlihat. Tidak ada coroutine tak berujung.
     */
    val state: StateFlow<RexMonitorState> = combine(
        repository.stream(DASHBOARD_INTERVAL_MS),
        RexMonitorService.status,
        local
    ) { m, svc, l ->
        RexMonitorState(
            device = m.device, cpu = m.cpu, gpu = m.gpu, ram = m.ram, storage = m.storage,
            battery = m.battery, temperature = m.temperature, network = m.network, system = m.system,
            health = computeHealth(m),
            monitoringStatus = svc,
            notificationPermission = l.notificationPermission,
            permissionDialog = l.dialog,
            rootAvailable = l.rootAvailable,
            rootGranted = l.rootGranted,
            booster = l.booster,
            loading = false,
            lastUpdatedMs = m.timestampMs,
            errors = m.errors
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RexMonitorState())

    init {
        refreshPermission()
        viewModelScope.launch(Dispatchers.IO) {
            val rooted = repository.rootChecker.isLikelyRooted()
            local.update { it.copy(rootAvailable = rooted) }
        }
    }

    // ---- Notification permission & monitoring ----

    private fun notificationsEnabled(): Boolean =
        NotificationManagerCompat.from(app).areNotificationsEnabled()

    fun refreshPermission() {
        local.update {
            it.copy(
                notificationPermission =
                    if (notificationsEnabled()) RexNotificationPermission.GRANTED
                    else RexNotificationPermission.DENIED
            )
        }
    }

    fun onMonitoringToggle(enabled: Boolean) {
        if (!enabled) {
            stopMonitoring()
            return
        }
        refreshPermission()
        if (notificationsEnabled()) {
            startMonitoring()
        } else {
            local.update {
                it.copy(
                    dialog = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        RexPermissionDialog.RATIONALE else RexPermissionDialog.DENIED
                )
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        refreshPermission()
        if (granted && notificationsEnabled()) {
            startMonitoring()
        } else {
            local.update { it.copy(dialog = RexPermissionDialog.DENIED) }
        }
    }

    fun dismissDialog() {
        local.update { it.copy(dialog = RexPermissionDialog.NONE) }
    }

    private fun startMonitoring() {
        RexMonitorService.markStarting()
        try {
            ContextCompat.startForegroundService(app, RexMonitorService.startIntent(app))
        } catch (_: Throwable) {
            RexMonitorService.markFailed()
            return
        }
        viewModelScope.launch {
            delay(START_TIMEOUT_MS)
            if (RexMonitorService.status.value == RexServiceStatus.STARTING) RexMonitorService.markFailed()
        }
    }

    private fun stopMonitoring() {
        app.stopService(RexMonitorService.startIntent(app))
    }

    // ---- Booster & root ----

    fun optimize() {
        if (local.value.booster.phase == RexBoosterPhase.RUNNING) return
        local.update { it.copy(booster = RexBoosterState(RexBoosterPhase.RUNNING)) }
        viewModelScope.launch {
            val result = try {
                booster.optimize(local.value.rootGranted)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            local.update {
                it.copy(booster = RexBoosterState(RexBoosterPhase.DONE, result, failed = result == null))
            }
        }
    }

    fun requestRoot() {
        viewModelScope.launch(Dispatchers.IO) {
            val granted = repository.rootChecker.requestSu()
            repository.setRootAllowed(granted)
            local.update { it.copy(rootGranted = granted) }
        }
    }

    override fun onCleared() {
        repository.close()
        super.onCleared()
    }

    private companion object {
        const val DASHBOARD_INTERVAL_MS = 1_000L
        const val START_TIMEOUT_MS = 6_000L
    }
}
