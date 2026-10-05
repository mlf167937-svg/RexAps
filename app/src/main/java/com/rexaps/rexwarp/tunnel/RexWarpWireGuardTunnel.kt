package com.rexaps.rexwarp.tunnel

import android.content.Context
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
import java.net.InetAddress
import java.net.SocketTimeoutException

private class HandshakeTimeoutException : IOException("WireGuard handshake timed out")

class RexWarpWireGuardProvider(private val context: Context) : RexWarpTunnelProvider {
    override val capabilities = RexWarpTunnelCapabilities(
        supportsIpv6 = true, supportsMtu = true, supportsKillSwitch = false
    )
    override val unavailableReason: String? = null
    override fun create(host: RexWarpTunnelHost): RexWarpTunnel = RexWarpWireGuardTunnel(context)
}

/**
 * Tunnel WARP via WireGuard (GoBackend). Tunnel dijalankan oleh VpnService milik library;
 * RexWarpVpnService tetap menjadi foreground service + pengelola state/statistik.
 * CONNECTED hanya jika handshake dengan server benar-benar terjadi.
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
            if (!awaitHandshake()) throw HandshakeTimeoutException()

            _info.value = RexWarpConnectionInfo(
                server = RexWarpServer("Cloudflare WARP", "${account.endpointHost}:${account.endpointPort}"),
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
            shutdown()
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

    private suspend fun awaitHandshake(): Boolean = withContext(Dispatchers.IO) {
        repeat(HANDSHAKE_TRIES) {
            if (hasHandshake()) return@withContext true
            delay(500)
        }
        hasHandshake()
    }

    private fun hasHandshake(): Boolean = runCatching {
        val s = backend.getStatistics(wgTunnel)
        s.peers().any { (s.peer(it)?.latestHandshakeEpochMillis() ?: 0L) > 0L }
    }.getOrDefault(false)

    private fun buildConfig(acc: RexWarpAccount, cfg: RexWarpTunnelConfig): Config {
        val iface = WgInterface.Builder().parsePrivateKey(acc.privateKey).setMtu(cfg.mtu ?: DEFAULT_MTU)
        if (cfg.ipv4) iface.addAddress(InetNetwork.parse("${acc.ipv4}/32"))
        if (cfg.ipv6 && acc.ipv6 != null) iface.addAddress(InetNetwork.parse("${acc.ipv6}/128"))
        cfg.dnsServers
            .filter { if (':' in it) cfg.ipv6 else cfg.ipv4 }
            .forEach { iface.addDnsServer(InetAddress.getByName(it)) } // literal IP, tanpa lookup

        val peer = Peer.Builder()
            .parsePublicKey(acc.peerPublicKey)
            .setEndpoint(InetEndpoint.parse("${acc.endpointHost}:${acc.endpointPort}"))
            .setPersistentKeepalive(25)
        if (cfg.ipv4) peer.addAllowedIp(InetNetwork.parse("0.0.0.0/0"))
        if (cfg.ipv6) peer.addAllowedIp(InetNetwork.parse("::/0"))

        return Config.Builder().setInterface(iface.build()).addPeer(peer.build()).build()
    }

    private fun mapError(e: Exception): RexWarpError = when (e) {
        is HandshakeTimeoutException, is SocketTimeoutException -> RexWarpError.TIMEOUT
        is BadConfigException -> RexWarpError.INVALID_CONFIG
        is IOException -> RexWarpError.NETWORK_UNAVAILABLE
        else -> RexWarpError.TUNNEL_FAILED
    }

    private companion object {
        const val DEFAULT_MTU = 1280
        const val HANDSHAKE_TRIES = 30 // 15 detik
    }
}