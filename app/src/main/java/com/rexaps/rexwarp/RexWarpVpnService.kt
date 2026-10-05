package com.rexaps.rexwarp

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.rexaps.rexwarp.tunnel.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import java.net.DatagramSocket
import java.net.Socket
import java.time.LocalDate

class RexWarpVpnService : VpnService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repo: RexWarpRepository
    private lateinit var cm: ConnectivityManager

    private var tunnel: RexWarpTunnel? = null
    private var connectJob: Job? = null
    private var stateJob: Job? = null
    private var infoJob: Job? = null
    private var monitorJob: Job? = null
    private var reconnectJob: Job? = null

    private var userWantsConnection = false
    private var paused = false
    private var stopping = false
    private var sessionActive = false
    private var failureInFlight = false
    private var reconnectAttempts = 0
    @Volatile private var settings = RexWarpSettings()

    private val physical = LinkedHashMap<Network, NetworkCapabilities>()
    private var lastRx = 0L; private var lastTx = 0L; private var lastSampleAt = 0L
    private var pendingDl = 0L; private var pendingUl = 0L; private var pendingDate: LocalDate = LocalDate.now()
    private var lastSpeed = RexWarpSpeed()
    private val dlHist = ArrayDeque<Long>()   // riwayat kecepatan untuk grafik notifikasi
    private val ulHist = ArrayDeque<Long>()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { physical[network] = caps; evaluatePolicy() }
        override fun onLost(network: Network) { physical.remove(network); evaluatePolicy() }
    }

    private inner class Host : RexWarpTunnelHost {
        override fun newBuilder(): Builder = this@RexWarpVpnService.Builder().setSession("RexWARP")
        override fun protect(fd: Int) = this@RexWarpVpnService.protect(fd)
        override fun protect(socket: Socket) = this@RexWarpVpnService.protect(socket)
        override fun protect(socket: DatagramSocket) = this@RexWarpVpnService.protect(socket)
    }

    override fun onCreate() {
        super.onCreate()
        repo = RexWarpRepository.get(applicationContext)
        cm = getSystemService(ConnectivityManager::class.java)
        RexWarpNotification.ensureChannel(this)
        cm.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN).build(),
            networkCallback, Handler(Looper.getMainLooper())
        )
        scope.launch { repo.preferences.settings.collect { settings = it; evaluatePolicy() } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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

    // ---- connect / disconnect ------------------------------------------------------------

    private fun startConnect() {
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
                Log.w(TAG, "connect failed", e)
                // Ambil penyebab spesifik dari tunnel (mis. WARP_NOT_ACTIVE, REGISTRATION_FAILED).
                val specific = (tunnel?.state?.value as? RexWarpTunnelState.Failed)?.error
                handleFailure(specific ?: RexWarpError.TUNNEL_FAILED)
            }
        }
    }

    private fun userDisconnect() {
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

    private fun handleFailure(error: RexWarpError) {
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

    private fun onTunnelState(s: RexWarpTunnelState) {
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

    private suspend fun teardownTunnel() {
        val t = tunnel ?: return
        tunnel = null; sessionActive = false
        runCatching { sample(t) }; flushPending()
        lastSpeed = RexWarpSpeed(); dlHist.clear(); ulHist.clear()
        monitorJob?.cancel(); stateJob?.cancel(); infoJob?.cancel(); connectJob?.cancel()
        runCatching { withTimeout(DISCONNECT_TIMEOUT_MS) { t.disconnect() } }
    }

    private fun finishService() {
        // Notifikasi error tetap terlihat setelah service berhenti (jangan dihapus bila ERROR).
        stopForeground(if (repo.state.value.connection == RexWarpConnectionState.ERROR) STOP_FOREGROUND_DETACH else STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ---- statistik & usage ---------------------------------------------------------------

    private fun startMonitor(t: RexWarpTunnel) {
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

    private fun sample(t: RexWarpTunnel) {
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

    private fun flushPending() {
        if (pendingDl == 0L && pendingUl == 0L) return
        val d = pendingDate; val dl = pendingDl; val ul = pendingUl
        pendingDl = 0; pendingUl = 0
        repo.appScope.launch { repo.usage.addTraffic(d, dl, ul) } // tetap jalan walau service sudah mati
    }

    // ---- aturan jaringan -----------------------------------------------------------------

    private fun hasNetwork(): Boolean {
        val n = cm.activeNetwork ?: return false
        return cm.getNetworkCapabilities(n)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private fun pauseReason(): String? {
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

    private fun evaluatePolicy() {
        val reason = pauseReason()
        if (reason != null && userWantsConnection && !paused && (tunnel != null || connectJob?.isActive == true)) {
            paused = true; stopping = true; connectJob?.cancel()
            scope.launch { teardownTunnel(); stopping = false; repo.setDisconnected(reason); updateNotification() }
        } else if (reason == null && paused && userWantsConnection && physical.isNotEmpty()) {
            paused = false; startConnect()
        }
    }

    // ---- notifikasi / lifecycle ----------------------------------------------------------

    /**
     * Android 14+: tipe SYSTEM_EXEMPTED bisa ditolak untuk app yang tidak memenuhi syarat.
     * Coba beberapa tipe berurutan supaya service tidak langsung mati.
     */
    private fun startForegroundSafely(): Boolean {
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
    private fun updateNotification(force: Boolean = true) {
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

    override fun onRevoke() {
        super.onRevoke()
        handleFailure(RexWarpError.PERMISSION_REVOKED)
    }

    override fun onDestroy() {
        runCatching { cm.unregisterNetworkCallback(networkCallback) }
        val t = tunnel
        if (t != null) { // pembersihan terakhir jika service dimatikan sistem
            runCatching { sample(t) }; flushPending()
            tunnel = null
            repo.appScope.launch { runCatching { withTimeout(DISCONNECT_TIMEOUT_MS) { t.disconnect() } } }
        }
        if (repo.state.value.connection != RexWarpConnectionState.ERROR) repo.setDisconnected()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_CONNECT = "com.rexaps.rexwarp.CONNECT"
        const val ACTION_DISCONNECT = "com.rexaps.rexwarp.DISCONNECT"
        private const val TAG = "RexWarp"
        private const val SAMPLE_MS = 1_000L
        private const val HISTORY_SIZE = 40
        private const val CONNECT_TIMEOUT_MS = 70_000L   // registrasi + handshake + verifikasi warp=on
        private const val DISCONNECT_TIMEOUT_MS = 5_000L
        private const val RECONNECT_BASE_DELAY_MS = 3_000L
        private const val MAX_RECONNECT = 5
        private val RETRYABLE = setOf(
            RexWarpError.UNEXPECTED_DISCONNECT, RexWarpError.NETWORK_UNAVAILABLE,
            RexWarpError.TIMEOUT, RexWarpError.TUNNEL_FAILED
        )

        fun start(ctx: Context) = ContextCompat.startForegroundService(
            ctx, Intent(ctx, RexWarpVpnService::class.java).setAction(ACTION_CONNECT)
        )
        fun stop(ctx: Context) {
            ctx.startService(Intent(ctx, RexWarpVpnService::class.java).setAction(ACTION_DISCONNECT))
        }
    }
}
