package com.rexaps.rexwarp.tunnel

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.rexaps.rexwarp.RexWarpError
import com.rexaps.rexwarp.model.RexWarpConnectionInfo
import com.rexaps.rexwarp.model.RexWarpServer
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel as WgTunnel
import com.wireguard.config.BadConfigException
import com.wireguard.config.Config
import com.wireguard.config.InetEndpoint
import com.wireguard.config.InetNetwork
import com.wireguard.config.Interface as WgInterface
import com.wireguard.config.Peer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URL

private class HandshakeTimeoutException : IOException("WireGuard handshake timed out")
private class WarpNotActiveException(val value: String) : IOException("Cloudflare trace says warp=$value")

/** Hasil https://www.cloudflare.com/cdn-cgi/trace yang dibaca LEWAT tunnel. */
private data class WarpTrace(val warp: String, val colo: String? = null, val ip: String? = null) {
    val isWarp: Boolean get() = warp == "on" || warp == "plus"
}

class RexWarpWireGuardProvider(private val context: Context) : RexWarpTunnelProvider {
    override val capabilities = RexWarpTunnelCapabilities(
        supportsIpv6 = true, supportsMtu = true, supportsKillSwitch = false
    )
    override val unavailableReason: String? = null
    override fun create(host: RexWarpTunnelHost): RexWarpTunnel = RexWarpWireGuardTunnel(context)
}

/**
 * Tunnel WARP via WireGuard (GoBackend).
 * Status CONNECTED HANYA diberikan jika Cloudflare sendiri membalas warp=on / warp=plus
 * pada permintaan yang dikirim lewat tunnel ini. Tidak ada koneksi palsu.
 */
class RexWarpWireGuardTunnel(context: Context) : RexWarpTunnel {
    private val app = context.applicationContext
    private val backend by lazy { GoBackend(app) }
    private val store = RexWarpAccountStore(app)

    private val _state = MutableStateFlow<RexWarpTunnelState>(RexWarpTunnelState.Disconnected)
    private val _info = MutableStateFlow(RexWarpConnectionInfo())
    override val state: StateFlow<RexWarpTunnelState> = _state.asStateFlow()
    override val connectionInfo: StateFlow<RexWarpConnectionInfo> = _info.asStateFlow()

    @Volatile private var closing = false
    @Volatile private var lastStats = RexWarpTunnelStats()

    private val wgTunnel = object : WgTunnel {
        override fun getName(): String = "rexwarp"
        override fun onStateChange(newState: WgTunnel.State) {
            if (newState == WgTunnel.State.DOWN && !closing && _state.value == RexWarpTunnelState.Connected) {
                _state.value = RexWarpTunnelState.Disconnected // diputus sistem / izin VPN dicabut
            }
        }
    }

    override suspend fun connect(config: RexWarpTunnelConfig) {
        closing = false
        _state.value = RexWarpTunnelState.Connecting
        try {
            val account = withContext(Dispatchers.IO) {
                val acc = store.load() ?: RexWarpRegistration.register().also { store.save(it) }
                backend.setState(wgTunnel, WgTunnel.State.UP, buildConfig(acc, config))
                acc
            }

            // Bukti nyata: tanya Cloudflare lewat tunnel. Lalu lintas ini juga memicu handshake WireGuard.
            val trace = waitForWarp()
            if (!trace.isWarp) {
                if (!hasHandshake()) throw HandshakeTimeoutException()
                throw WarpNotActiveException(trace.warp)
            }

            _info.value = RexWarpConnectionInfo(
                server = RexWarpServer(
                    name = "Cloudflare WARP" + (trace.colo?.let { " · $it" } ?: ""),
                    endpoint = "${account.endpointHost}:${account.endpointPort}"
                ),
                protocol = "WireGuard",
                ipv4 = account.ipv4.takeIf { config.ipv4 },
                ipv6 = account.ipv6.takeIf { config.ipv6 },
                dnsServers = config.dnsServers,
                latencyMs = null,
                mtu = config.mtu ?: DEFAULT_MTU
            )
            _state.value = RexWarpTunnelState.Connected
        } catch (e: CancellationException) {
            shutdown()
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "connect failed: ${e.javaClass.simpleName}: ${e.message}", e)
            shutdown()
            // Kredensial lama mungkin sudah tidak diterima server -> daftar ulang di percobaan berikutnya.
            if (e is HandshakeTimeoutException) store.clear()
            _state.value = RexWarpTunnelState.Failed(mapError(e))
            throw e
        }
    }

    override suspend fun disconnect() {
        closing = true
        if (_state.value !is RexWarpTunnelState.Failed) _state.value = RexWarpTunnelState.Disconnecting
        shutdown()
        if (_state.value !is RexWarpTunnelState.Failed) _state.value = RexWarpTunnelState.Disconnected
    }

    override fun isConnected(): Boolean = _state.value == RexWarpTunnelState.Connected

    override fun getConnectionInfo(): RexWarpConnectionInfo = _info.value

    override fun getStats(): RexWarpTunnelStats = runCatching {
        val s = backend.getStatistics(wgTunnel)
        RexWarpTunnelStats(s.totalRx(), s.totalTx())
    }.onSuccess { lastStats = it }.getOrDefault(lastStats)

    // ---- internal ----

    private suspend fun shutdown() {
        closing = true
        withContext(NonCancellable + Dispatchers.IO) {
            runCatching { backend.setState(wgTunnel, WgTunnel.State.DOWN, null) }
        }
    }

    private fun hasHandshake(): Boolean = runCatching {
        val s = backend.getStatistics(wgTunnel)
        s.peers().any { (s.peer(it)?.latestHandshakeEpochMillis() ?: 0L) > 0L }
    }.getOrDefault(false)

    /** Ulangi sampai Cloudflare menjawab warp=on/plus, maksimal [WARP_CHECK_TOTAL_MS]. */
    private suspend fun waitForWarp(): WarpTrace = withContext(Dispatchers.IO) {
        var last = WarpTrace("unreachable")
        val deadline = SystemClock.elapsedRealtime() + WARP_CHECK_TOTAL_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            last = fetchTrace()
            if (last.isWarp) break
            delay(1_000)
        }
        Log.i(TAG, "trace result: $last")
        last
    }

    private fun fetchTrace(): WarpTrace {
        return try {
            val c = (URL(TRACE_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4_000
                readTimeout = 4_000
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache")
            }
            try {
                val text = c.inputStream.bufferedReader().use { it.readText() }
                val map = text.lineSequence()
                    .mapNotNull { l -> l.indexOf('=').takeIf { it > 0 }?.let { l.substring(0, it) to l.substring(it + 1).trim() } }
                    .toMap()
                WarpTrace(map["warp"] ?: "missing", map["colo"], map["ip"])
            } finally {
                c.disconnect()
            }
        } catch (e: Exception) {
            WarpTrace("error:${e.javaClass.simpleName}")
        }
    }

    private fun buildConfig(acc: RexWarpAccount, cfg: RexWarpTunnelConfig): Config {
        val iface = WgInterface.Builder().parsePrivateKey(acc.privateKey).setMtu(cfg.mtu ?: DEFAULT_MTU)
        if (cfg.ipv4) iface.addAddress(InetNetwork.parse("${acc.ipv4}/32"))
        if (cfg.ipv6 && acc.ipv6 != null) iface.addAddress(InetNetwork.parse("${acc.ipv6}/128"))
        cfg.dnsServers
            .filter { if (':' in it) cfg.ipv6 else cfg.ipv4 }
            .forEach { iface.addDnsServer(InetAddress.getByName(it)) } // literal IP, tanpa lookup

        val peer = Peer.Builder()
            .parsePublicKey(acc.peerPublicKey)
            // Selalu pakai IP endpoint terbaru; akun lama mungkin masih menyimpan nama host.
            .setEndpoint(InetEndpoint.parse("${RexWarpRegistration.ENDPOINT_HOST}:${acc.endpointPort}"))
            .setPersistentKeepalive(25)
        if (cfg.ipv4) peer.addAllowedIp(InetNetwork.parse("0.0.0.0/0"))
        if (cfg.ipv6) peer.addAllowedIp(InetNetwork.parse("::/0"))

        return Config.Builder().setInterface(iface.build()).addPeer(peer.build()).build()
    }

    /** Membaca BackendException.Reason lewat refleksi supaya tidak bergantung versi library. */
    private fun backendReason(e: Exception): String? =
        if (e.javaClass.simpleName == "BackendException")
            runCatching { e.javaClass.getMethod("getReason").invoke(e)?.toString() }.getOrNull()
        else null

    private fun mapError(e: Exception): RexWarpError = when {
        e is WarpNotActiveException -> RexWarpError.WARP_NOT_ACTIVE
        e is RexWarpRegistrationException -> RexWarpError.REGISTRATION_FAILED
        e is HandshakeTimeoutException || e is SocketTimeoutException -> RexWarpError.TIMEOUT
        e is BadConfigException -> RexWarpError.INVALID_CONFIG
        backendReason(e) == "VPN_NOT_AUTHORIZED" -> RexWarpError.PERMISSION_REVOKED
        backendReason(e) == "DNS_RESOLUTION_FAILURE" -> RexWarpError.DNS_FAILURE
        e is IOException -> RexWarpError.NETWORK_UNAVAILABLE
        else -> RexWarpError.TUNNEL_FAILED
    }

    private companion object {
        const val TAG = "RexWarp"
        const val TRACE_URL = "https://www.cloudflare.com/cdn-cgi/trace"
        const val DEFAULT_MTU = 1280
        const val WARP_CHECK_TOTAL_MS = 25_000L
    }
}
