package com.rexaps.rexwarp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.*
import com.rexaps.rexwarp.ui.components.RexWarpInfoRow

@Composable
fun RexWarpDetailsScreen(state: RexWarpState, modifier: Modifier = Modifier) {
    val connected = state.connection == RexWarpConnectionState.CONNECTED
    val seconds by rememberSessionSeconds(state.sessionStartElapsedMs, connected)
    val i = state.info.takeIf { connected }
    val na = "Not available"
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                RexWarpInfoRow("Connection status", state.connection.name)
                RexWarpInfoRow("Tunnel state", state.connection.name.lowercase().replaceFirstChar { it.uppercase() })
                RexWarpInfoRow("Protocol", i?.let { it.protocol ?: "Unknown" } ?: na)
                RexWarpInfoRow("Endpoint", i?.server?.endpoint ?: na)
                RexWarpInfoRow("IPv4", i?.ipv4 ?: na)
                RexWarpInfoRow("IPv6", i?.ipv6 ?: na)
                RexWarpInfoRow("DNS", i?.dnsServers?.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: na)
                RexWarpInfoRow("Latency", i?.latencyMs?.let { "$it ms" } ?: na)
                RexWarpInfoRow("MTU", i?.mtu?.toString() ?: na)
                RexWarpInfoRow("Session duration", seconds?.let(RexWarpFormat::duration) ?: na)
                RexWarpInfoRow("Bytes received", state.stats?.takeIf { connected }?.let { RexWarpFormat.bytes(it.rxBytes) } ?: na)
                RexWarpInfoRow("Bytes sent", state.stats?.takeIf { connected }?.let { RexWarpFormat.bytes(it.txBytes) } ?: na)
            }
        }
    }
}
