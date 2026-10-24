package com.rexaps.rexwarp

import android.content.Context
import android.content.Intent
import android.net.*
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.rexaps.rexwarp.tunnel.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.net.DatagramSocket
import java.net.Socket
import java.time.LocalDate

class RexWarpVpnService : VpnService() {

    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    internal lateinit var repo: RexWarpRepository
    internal lateinit var cm: ConnectivityManager

    internal var tunnel: RexWarpTunnel? = null
    internal var connectJob: Job? = null
    internal var stateJob: Job? = null
    internal var infoJob: Job? = null
    private var monitorJob: Job? = null
    internal var reconnectJob: Job? = null

    internal var userWantsConnection = false
    internal var paused = false
    internal var stopping = false
    internal var sessionActive = false
    internal var failureInFlight = false
    internal var reconnectAttempts = 0
    @Volatile internal var settings = RexWarpSettings()

    internal val physical = LinkedHashMap<Network, NetworkCapabilities>()
    internal var lastRx = 0L; internal var lastTx = 0L; internal var lastSampleAt = 0L
    internal var pendingDl = 0L; internal var pendingUl = 0L; internal var pendingDate: LocalDate = LocalDate.now()
    internal var lastSpeed = RexWarpSpeed()
    internal val dlHist = ArrayDeque<Long>()   // riwayat kecepatan untuk grafik notifikasi
    internal val ulHist = ArrayDeque<Long>()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { physical[network] = caps; evaluatePolicy() }
        override fun onLost(network: Network) { physical.remove(network); evaluatePolicy() }
    }

    internal inner class Host : RexWarpTunnelHost {
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

    internal fun startConnect() {
        return this.__splitImpl_startConnect()
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

    internal fun handleFailure(error: RexWarpError) {
        return this.__splitImpl_handleFailure(error)
    }

    internal fun onTunnelState(s: RexWarpTunnelState) {
        return this.__splitImpl_onTunnelState(s)
    }

    internal suspend fun teardownTunnel() {
        val t = tunnel ?: return
        tunnel = null; sessionActive = false
        runCatching { sample(t) }; flushPending()
        lastSpeed = RexWarpSpeed(); dlHist.clear(); ulHist.clear()
        monitorJob?.cancel(); stateJob?.cancel(); infoJob?.cancel(); connectJob?.cancel()
        runCatching { withTimeout(DISCONNECT_TIMEOUT_MS) { t.disconnect() } }
    }

    internal fun finishService() {
        // Notifikasi error tetap terlihat setelah service berhenti (jangan dihapus bila ERROR).
        stopForeground(if (repo.state.value.connection == RexWarpConnectionState.ERROR) STOP_FOREGROUND_DETACH else STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ---- statistik & usage ---------------------------------------------------------------

    internal fun startMonitor(t: RexWarpTunnel) {
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

    internal fun sample(t: RexWarpTunnel) {
        return this.__splitImpl_sample(t)
    }

    internal fun flushPending() {
        if (pendingDl == 0L && pendingUl == 0L) return
        val d = pendingDate; val dl = pendingDl; val ul = pendingUl
        pendingDl = 0; pendingUl = 0
        repo.appScope.launch { repo.usage.addTraffic(d, dl, ul) } // tetap jalan walau service sudah mati
    }

    // ---- aturan jaringan -----------------------------------------------------------------

    internal fun hasNetwork(): Boolean {
        val n = cm.activeNetwork ?: return false
        return cm.getNetworkCapabilities(n)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    internal fun pauseReason(): String? {
        return this.__splitImpl_pauseReason()
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
    internal fun startForegroundSafely(): Boolean {
        return this.__splitImpl_startForegroundSafely()
    }

    /** [force] = false: lewati bila layar mati (hemat baterai); notifikasi segar lagi saat layar menyala. */
    internal fun updateNotification(force: Boolean = true) {
        return this.__splitImpl_updateNotification(force)
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
        internal const val TAG = "RexWarp"
        private const val SAMPLE_MS = 1_000L
        internal const val HISTORY_SIZE = 40
        internal const val CONNECT_TIMEOUT_MS = 70_000L   // registrasi + handshake + verifikasi warp=on
        private const val DISCONNECT_TIMEOUT_MS = 5_000L
        internal const val RECONNECT_BASE_DELAY_MS = 3_000L
        internal const val MAX_RECONNECT = 5
        internal val RETRYABLE = setOf(
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
