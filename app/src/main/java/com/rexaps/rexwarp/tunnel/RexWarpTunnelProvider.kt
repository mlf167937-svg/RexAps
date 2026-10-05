package com.rexaps.rexwarp.tunnel

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

/** Default: tidak ada engine. Tidak pernah menghasilkan koneksi palsu. */
object NoEngineTunnelProvider : RexWarpTunnelProvider {
    override val capabilities = RexWarpTunnelCapabilities()
    override val unavailableReason = "No WARP tunnel engine is installed in this build."
    override fun create(host: RexWarpTunnelHost): RexWarpTunnel =
        error("No tunnel engine available")
}

object RexWarpTunnelRegistry {
    /** Set dari Application.onCreate() bila engine sudah diintegrasikan. */
    @Volatile var provider: RexWarpTunnelProvider = NoEngineTunnelProvider
}