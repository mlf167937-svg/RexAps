package com.rexaps.rexwarp.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.rexaps.rexwarp.RexWarpConnectionState
import com.rexaps.rexwarp.RexWarpFormat
import com.rexaps.rexwarp.RexWarpState

@Composable
fun RexWarpDetailsScreen(state: RexWarpState, modifier: Modifier = Modifier, onConnectClick: (() -> Unit)? = null) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val connected = state.connection == RexWarpConnectionState.CONNECTED
    val seconds by rememberSessionSeconds(state.sessionStartElapsedMs, connected)
    if (landscape) RexWarpDetailsScreenLandscape(state, seconds, modifier, onConnectClick)
    else RexWarpDetailsScreenPortrait(state, seconds, modifier, onConnectClick)
}

@Composable
internal fun RexWarpDetailsConnectionRows(state: RexWarpState, seconds: Long?) {
    val connected = state.connection == RexWarpConnectionState.CONNECTED
    val info = state.info.takeIf { connected }
    val na = "Not available"
    com.rexaps.rexwarp.ui.components.RexWarpInfoRow("Connection status", state.connection.name)
    com.rexaps.rexwarp.ui.components.RexWarpInfoRow("Tunnel state", state.connection.name.lowercase().replaceFirstChar { it.uppercase() })
    com.rexaps.rexwarp.ui.components.RexWarpInfoRow("Session duration", seconds?.let(RexWarpFormat::duration) ?: na)
    com.rexaps.rexwarp.ui.components.RexWarpInfoRow("Latency", info?.latencyMs?.let { "$it ms" } ?: na)
}

@Composable
internal fun RexWarpDetailsNetworkRows(state: RexWarpState) {
    val connected = state.connection == RexWarpConnectionState.CONNECTED
    val info = state.info.takeIf { connected }
    val na = "Not available"
    com.rexaps.rexwarp.ui.components.RexWarpInfoRow("Protocol", info?.let { it.protocol ?: "Unknown" } ?: na)
    com.rexaps.rexwarp.ui.components.RexWarpInfoRow("Endpoint", info?.server?.endpoint ?: na)
    com.rexaps.rexwarp.ui.components.RexWarpInfoRow("IPv4", info?.ipv4 ?: na)
    com.rexaps.rexwarp.ui.components.RexWarpInfoRow("IPv6", info?.ipv6 ?: na)
    com.rexaps.rexwarp.ui.components.RexWarpInfoRow("DNS", info?.dnsServers?.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: na)
    com.rexaps.rexwarp.ui.components.RexWarpInfoRow("MTU", info?.mtu?.toString() ?: na)
    com.rexaps.rexwarp.ui.components.RexWarpInfoRow("Bytes received", state.stats?.takeIf { connected }?.let { RexWarpFormat.bytes(it.rxBytes) } ?: na)
    com.rexaps.rexwarp.ui.components.RexWarpInfoRow("Bytes sent", state.stats?.takeIf { connected }?.let { RexWarpFormat.bytes(it.txBytes) } ?: na)
}
