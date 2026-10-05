package com.rexaps.rexwarp

import com.rexaps.rexwarp.model.RexWarpConnectionInfo
import com.rexaps.rexwarp.tunnel.RexWarpTunnelStats

enum class RexWarpConnectionState { DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING, ERROR }

enum class RexWarpError(val userMessage: String) {
    PERMISSION_DENIED("VPN permission is required to start RexWARP."),
    PERMISSION_REVOKED("VPN permission was revoked. Connect again to grant it."),
    TUNNEL_FAILED("Unable to establish the tunnel."),
    NETWORK_UNAVAILABLE("Network unavailable."),
    TIMEOUT("The connection timed out. Please try again."),
    DNS_FAILURE("DNS could not be resolved. Check your DNS settings."),
    SERVICE_STOPPED("The RexWARP service was stopped."),
    UNEXPECTED_DISCONNECT("The connection was lost unexpectedly."),
    INVALID_CONFIG("Connection settings are invalid. Check DNS, IP and MTU settings."),
    UNSUPPORTED_ANDROID("This Android version is not supported."),
    ENGINE_UNAVAILABLE("The WARP tunnel engine is not installed in this build.")
}

data class RexWarpState(
    val connection: RexWarpConnectionState = RexWarpConnectionState.DISCONNECTED,
    val error: RexWarpError? = null,
    val pausedReason: String? = null,
    val info: RexWarpConnectionInfo = RexWarpConnectionInfo(),
    val speed: RexWarpSpeed = RexWarpSpeed(),
    val stats: RexWarpTunnelStats? = null,
    val sessionStartElapsedMs: Long? = null,
    val engineUnavailableReason: String? = null
)