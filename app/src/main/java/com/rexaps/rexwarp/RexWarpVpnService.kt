package com.rexaps.rexwarp

import android.content.Context
import android.content.Intent
import android.net.*
import android.os.Handler
import android.os.Looper
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
    internal var monitorJob: Job? = null
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = this.__split_onStartCommand_1(intent, flags, startId)

    // ---- connect / disconnect ------------------------------------------------------------

    internal fun startConnect() = this.__split_startConnect_2()

    internal fun userDisconnect() = this.__split_userDisconnect_3()

    internal fun handleFailure(error: RexWarpError) = this.__split_handleFailure_4(error)

    internal fun onTunnelState(s: RexWarpTunnelState) = this.__split_onTunnelState_5(s)

    internal suspend fun teardownTunnel() = this.__split_teardownTunnel_6()

    internal fun finishService() = this.__split_finishService_7()

    // ---- statistik & usage ---------------------------------------------------------------

    internal fun startMonitor(t: RexWarpTunnel) = this.__split_startMonitor_8(t)

    internal fun sample(t: RexWarpTunnel) = this.__split_sample_9(t)

    internal fun flushPending() = this.__split_flushPending_10()

    // ---- aturan jaringan -----------------------------------------------------------------

    internal fun hasNetwork(): Boolean = this.__splitExtra_hasNetwork_1()

    internal fun pauseReason(): String? = this.__split_pauseReason_11()

    private fun evaluatePolicy() = this.__split_evaluatePolicy_12()

    // ---- notifikasi / lifecycle ----------------------------------------------------------

    /**
     * Android 14+: tipe SYSTEM_EXEMPTED bisa ditolak untuk app yang tidak memenuhi syarat.
     * Coba beberapa tipe berurutan supaya service tidak langsung mati.
     */
    internal fun startForegroundSafely(): Boolean = this.__split_startForegroundSafely_13()

    /** [force] = false: lewati bila layar mati (hemat baterai); notifikasi segar lagi saat layar menyala. */
    internal fun updateNotification(force: Boolean = true) = this.__split_updateNotification_14(force)

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
        internal const val SAMPLE_MS = 1_000L
        internal const val HISTORY_SIZE = 40
        internal const val CONNECT_TIMEOUT_MS = 70_000L   // registrasi + handshake + verifikasi warp=on
        internal const val DISCONNECT_TIMEOUT_MS = 5_000L
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
