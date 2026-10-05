package com.rexaps.rexwarp.model
data class RexWarpConnectionInfo(
    val server: RexWarpServer? = null,
    val protocol: String? = null,
    val ipv4: String? = null,
    val ipv6: String? = null,
    val dnsServers: List<String> = emptyList(),
    val latencyMs: Int? = null,
    val mtu: Int? = null
)
