package com.rexaps.rexwarp.tunnel

import android.content.Context

data class RexWarpTunnelCapabilities(
    val supportsIpv6: Boolean = false,
    val supportsMtu: Boolean = false,
    val supportsKillSwitch: Boolean = false
)

interface RexWarpTunnelProvider {
    val capabilities: RexWarpTunnelCapabilities
    /** null = engine tersedia. Selain itu, alasan yang ditampilkan ke user. */
    val unavailableReason: String?
    fun create(host: RexWarpTunnelHost): RexWarpTunnel
}

/** Fallback: tidak ada engine. Tidak pernah menghasilkan koneksi palsu. */
object NoEngineTunnelProvider : RexWarpTunnelProvider {
    override val capabilities = RexWarpTunnelCapabilities()
    override val unavailableReason = "No WARP tunnel engine is installed in this build."
    override fun create(host: RexWarpTunnelHost): RexWarpTunnel = error("No tunnel engine available")
}

object RexWarpTunnelRegistry {
    @Volatile var provider: RexWarpTunnelProvider = NoEngineTunnelProvider

    /** Memasang engine WireGuard bila belum ada provider lain. Dipanggil otomatis oleh RexWarpRepository. */
    @Synchronized
    fun installDefault(context: Context) {
        if (provider === NoEngineTunnelProvider) {
            provider = RexWarpWireGuardProvider(context.applicationContext)
        }
    }
}