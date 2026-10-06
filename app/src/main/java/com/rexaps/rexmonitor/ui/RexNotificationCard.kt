package com.rexaps.rexmonitor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.rexaps.rexmonitor.RexFormat
import com.rexaps.rexmonitor.RexMonitorState
import com.rexaps.rexmonitor.monitoring.RexServiceStatus

@Composable
fun RexNotificationCard(
    state: RexMonitorState,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = RexTheme.palette
    val status = state.monitoringStatus
    // Switch hanya ON jika service benar-benar aktif / sedang start.
    val checked = status == RexServiceStatus.RUNNING || status == RexServiceStatus.STARTING
    val subtitle = when (status) {
        RexServiceStatus.STOPPED -> "Menampilkan status perangkat di notification bar"
        RexServiceStatus.STARTING -> "Starting monitoring..."
        RexServiceStatus.RUNNING -> "Aktif"
        RexServiceStatus.FAILED -> "Monitoring unavailable"
    }
    RexCard(modifier, p.cyan, "Monitoring Notification. $subtitle") {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onToggle),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RexIconBadge(Icons.Rounded.Notifications, p.cyan)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Monitoring Notification", style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status == RexServiceStatus.FAILED) p.red else p.textSecondary
                )
            }
            Switch(checked = checked, onCheckedChange = null)
        }
        if (status == RexServiceStatus.RUNNING && !state.loading) {
            Spacer(Modifier.height(8.dp))
            val ram = state.ram
            Text(
                listOfNotNull(
                    if (ram.usedBytes != null && ram.totalBytes != null)
                        "RAM ${RexFormat.gbValue(ram.usedBytes!!)}/${RexFormat.gbValue(ram.totalBytes!!)} GB" else null,
                    state.cpu.usagePercent?.let { "CPU ${it.toInt()}%" },
                    (state.temperature.cpuC ?: state.temperature.batteryC)?.let { "Temperature ${it.toInt()}°C" },
                    state.battery.percent?.let { "Battery $it%" }
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = p.textSecondary
            )
        }
    }
}
