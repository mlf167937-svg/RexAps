package com.rexaps.rexwarp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.RexWarpConnectionState as S
import com.rexaps.rexwarp.RexWarpFormat
import com.rexaps.rexwarp.RexWarpState

/** Status ditulis sebagai teks + titik; warna bukan satu-satunya penanda. */
@Composable
fun RexWarpStatusBadge(state: S, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val dot = when (state) { S.CONNECTED -> c.primary; S.ERROR -> c.error; S.CONNECTING, S.DISCONNECTING -> c.tertiary; S.DISCONNECTED -> c.outline }
    Surface(shape = CircleShape, color = c.surfaceVariant, modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(10.dp).background(dot, CircleShape))
            Text(state.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun RexWarpInfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End, modifier = Modifier.weight(1.4f))
    }
}

@Composable
fun RexWarpStatusCard(state: RexWarpState, sessionSeconds: Long?, onOpenDetails: () -> Unit, modifier: Modifier = Modifier) {
    val connected = state.connection == S.CONNECTED
    val i = state.info.takeIf { connected }
    val na = "N/A"
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Connection", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            RexWarpInfoRow("Status", state.connection.name)
            RexWarpInfoRow("Duration", sessionSeconds?.let(RexWarpFormat::duration) ?: na)
            RexWarpInfoRow("Server / endpoint", i?.server?.endpoint ?: na)
            RexWarpInfoRow("Protocol", i?.let { it.protocol ?: "Unknown" } ?: na)
            RexWarpInfoRow("Latency", i?.latencyMs?.let { "$it ms" } ?: na)
            RexWarpInfoRow("DNS", i?.dnsServers?.takeIf { it.isNotEmpty() }?.let { "Active · " + it.joinToString(", ") } ?: na)
            RexWarpInfoRow("IPv4", i?.ipv4 ?: na)
            i?.ipv6?.let { RexWarpInfoRow("IPv6", it) }
            TextButton(onClick = onOpenDetails, modifier = Modifier.align(Alignment.End)) { Text("Connection details") }
        }
    }
}