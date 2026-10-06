package com.rexaps.rexmonitor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rexaps.rexmonitor.RexFormat
import com.rexaps.rexmonitor.device.RexDeviceInfo
import com.rexaps.rexmonitor.device.RexNetworkInfo
import com.rexaps.rexmonitor.device.RexSystemInfo
import com.rexaps.rexmonitor.device.RexTemperatureInfo

@Composable
fun RexSystemCard(
    device: RexDeviceInfo,
    system: RexSystemInfo,
    network: RexNetworkInfo,
    temperature: RexTemperatureInfo,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    val p = RexTheme.palette
    val connection = when {
        network.isConnected == null -> REX_UNAVAILABLE
        network.isConnected == false -> "Terputus"
        network.validated == true -> "Stabil"
        else -> "Terbatas"
    }
    val apps = system.installedAppCount?.let {
        if (system.appCountLimited) "$it terlihat" else "$it"
    } ?: REX_UNAVAILABLE
    RexCard(modifier, p.blue, "Informasi sistem") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RexIconBadge(Icons.Rounded.Android, p.blue)
            Spacer(Modifier.width(10.dp))
            Text("Sistem", style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
        }
        Spacer(Modifier.height(8.dp))
        Column {
            RexLabelValue("Uptime", if (loading) "…" else system.uptimeMs?.let { RexFormat.uptime(it) } ?: REX_UNAVAILABLE)
            RexLabelValue("Android", device.androidVersion?.let { "$it (API ${device.sdkInt})" } ?: REX_UNAVAILABLE)
            RexLabelValue("Arsitektur", device.architecture ?: REX_UNAVAILABLE)
            RexLabelValue("Aplikasi terpasang", if (loading) "…" else apps)
            RexLabelValue("Koneksi", if (loading) "…" else connection)
            RexLabelValue("Status termal", if (loading) "…" else thermalLabel(temperature.thermalLevel))
        }
        if (system.appCountLimited) {
            Text(
                "Android 11+ membatasi daftar aplikasi yang terlihat; jumlah bisa kurang dari sebenarnya.",
                style = MaterialTheme.typography.bodySmall,
                color = p.textSecondary
            )
        }
    }
}
