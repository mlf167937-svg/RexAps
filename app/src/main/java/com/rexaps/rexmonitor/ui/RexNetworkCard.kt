package com.rexaps.rexmonitor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rexaps.rexmonitor.RexFormat
import com.rexaps.rexmonitor.device.RexNetworkInfo

@Composable
fun RexNetworkCard(net: RexNetworkInfo, loading: Boolean, modifier: Modifier = Modifier) {
    val p = RexTheme.palette
    val down = RexFormat.mbps(net.downloadBps)
    val up = RexFormat.mbps(net.uploadBps)
    val wifiState = if (net.wifiConnected) "Tersambung" else "Tidak tersambung"
    val cellState = when {
        !net.cellularConnected -> "Tidak aktif"
        else -> net.cellularType ?: "Tersambung"
    }
    val desc = "Jaringan. Wi-Fi $wifiState, seluler $cellState, unduh ${down ?: REX_UNAVAILABLE}, unggah ${up ?: REX_UNAVAILABLE}"
    RexCard(modifier, p.cyan, desc) {
        Text("Jaringan", style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LinkBlock(
                icon = Icons.Rounded.Wifi, title = "Wi-Fi", accent = p.cyan,
                lines = buildList {
                    add(if (loading) "…" else wifiState)
                    if (net.wifiConnected) add(net.wifiRssiDbm?.let { "$it dBm" } ?: "Sinyal $REX_UNAVAILABLE")
                },
                modifier = Modifier.weight(1f)
            )
            LinkBlock(
                icon = Icons.Rounded.SignalCellularAlt, title = "Seluler", accent = p.green,
                lines = buildList {
                    add(if (loading) "…" else cellState)
                    if (net.cellularConnected) {
                        net.operatorName?.let { add(it) }
                        if (net.cellularType == null) add("Tipe jaringan tidak tersedia")
                        net.cellularSignalDbm?.let { add("$it dBm") }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SpeedBlock(Icons.Rounded.Download, "Download", down, p.cyan, loading, Modifier.weight(1f))
            SpeedBlock(Icons.Rounded.Upload, "Upload", up, p.magenta, loading, Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "${net.trafficScope} — total traffic perangkat, bukan traffic tunnel RexWARP",
            style = MaterialTheme.typography.bodySmall,
            color = p.textSecondary
        )
    }
}

@Composable
private fun LinkBlock(icon: ImageVector, title: String, accent: Color, lines: List<String>, modifier: Modifier) {
    val p = RexTheme.palette
    Row(modifier, verticalAlignment = Alignment.Top) {
        RexIconBadge(icon, accent)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, color = p.textPrimary)
            lines.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = p.textSecondary) }
        }
    }
}

@Composable
private fun SpeedBlock(icon: ImageVector, label: String, value: String?, accent: Color, loading: Boolean, modifier: Modifier) {
    val p = RexTheme.palette
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = accent)
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = p.textSecondary)
            Text(
                if (loading) "…" else value ?: "Mengukur…",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = p.textPrimary
            )
        }
    }
}
