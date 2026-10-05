package com.rexaps.rexwarp.tunnel

import android.net.VpnService
import com.rexaps.rexwarp.model.RexWarpConnectionInfo
import kotlinx.coroutines.flow.StateFlow
import java.net.DatagramSocket
import java.net.Socket

/** Counter kumulatif sejak sesi mulai. rx = download, tx = upload (payload lewat tunnel). */
data class RexWarpTunnelStats(val rxBytes: Long = 0, val txBytes: Long = 0)

data class RexWarpTunnelConfig(
    val dnsServers: List<String>,   // kosong = pakai DNS sistem
    val ipv4: Boolean,
    val ipv6: Boolean,
    val mtu: Int?,                  // null = otomatis
    val killSwitch: Boolean
)

/**
 * Kontrak yang disediakan service kepada engine.
 * Engine membuat interface TUN sendiri lewat [newBuilder] SETELAH siap meneruskan paket,
 * sehingga rute default tidak pernah ada tanpa engine yang hidup.
 */
interface RexWarpTunnelHost {
    fun newBuilder(): VpnService.Builder
    fun protect(fd: Int): Boolean
    fun protect(socket: Socket): Boolean
    fun protect(socket: DatagramSocket): Boolean
}

/**
 * Kontrak engine:
 * - connect() suspend sampai benar-benar Connected, atau melempar exception / state Failed.
 * - state HANYA boleh Connected jika paket benar-benar bisa diteruskan.
 * - disconnect() idempotent dan harus menutup TUN fd serta semua socket.
 */
interface RexWarpTunnel {
    val state: StateFlow<RexWarpTunnelState>
    val connectionInfo: StateFlow<RexWarpConnectionInfo>
    suspend fun connect(config: RexWarpTunnelConfig)
    suspend fun disconnect()
    fun isConnected(): Boolean
    fun getConnectionInfo(): RexWarpConnectionInfo
    fun getStats(): RexWarpTunnelStats
}