package com.rexaps.rexmonitor.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rexaps.rexmonitor.RexBoosterPhase
import com.rexaps.rexmonitor.RexBoosterState
import com.rexaps.rexmonitor.RexFormat
import com.rexaps.rexmonitor.RexHealth
import com.rexaps.rexmonitor.RexHealthLevel
import com.rexaps.rexmonitor.booster.RexCheckStatus
import com.rexaps.rexmonitor.booster.RexOptimizationResult

@Composable
fun RexBoosterCard(
    health: RexHealth,
    booster: RexBoosterState,
    rootAvailable: Boolean,
    rootGranted: Boolean,
    onOptimize: () -> Unit,
    onRequestRoot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = RexTheme.palette
    val context = LocalContext.current
    val running = booster.phase == RexBoosterPhase.RUNNING
    val condition = when (health.level) {
        RexHealthLevel.NORMAL -> "Good"
        RexHealthLevel.WARM -> "Fair"
        RexHealthLevel.ATTENTION -> "Needs attention"
    }
    RexCard(modifier, p.magenta, "RexBooster. Kondisi perangkat $condition") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RexIconBadge(Icons.Rounded.RocketLaunch, p.magenta)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("RexBooster", style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
                Text("Device condition: $condition", style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
            }
        }
        Spacer(Modifier.height(10.dp))

        val result = booster.result
        when {
            booster.phase == RexBoosterPhase.DONE && booster.failed ->
                Text("Optimization unavailable", style = MaterialTheme.typography.bodyMedium, color = p.red)
            booster.phase == RexBoosterPhase.DONE && result != null -> ResultBlock(result)
            else -> {
                Text("Available optimizations", style = MaterialTheme.typography.labelLarge, color = p.textSecondary)
                listOf("RexAps background tasks & cache lama", "Storage health", "Thermal check", "Memory check")
                    .forEach { Text("✓ $it", style = MaterialTheme.typography.bodySmall, color = p.textPrimary) }
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(onClick = onOptimize, enabled = !running, modifier = Modifier.fillMaxWidth()) {
            if (running) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Memeriksa…")
            } else Text("Optimize")
        }

        if (result?.suggestStorageSettings == true) {
            OutlinedButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Buka pengaturan penyimpanan") }
        }

        if (rootAvailable) {
            Spacer(Modifier.height(10.dp))
            Text(
                if (rootGranted) "Advanced mode • Root diizinkan (hanya baca statistik CPU)"
                else "Root detected • Advanced features may be available",
                style = MaterialTheme.typography.bodySmall,
                color = p.textSecondary
            )
            if (!rootGranted) {
                OutlinedButton(onClick = onRequestRoot, modifier = Modifier.fillMaxWidth()) {
                    Text("Izinkan root (opsional) untuk membaca CPU usage")
                }
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Text("Non-root device • Standard features available", style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
        }
    }
}

@Composable
private fun ResultBlock(r: RexOptimizationResult) {
    val p = RexTheme.palette
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Optimization complete", style = MaterialTheme.typography.titleSmall, color = p.textPrimary)
        Text("✓ ${r.completedChecks} checks completed", style = MaterialTheme.typography.bodySmall, color = p.textPrimary)
        if (r.appliedCount > 0) {
            Text("✓ ${r.appliedCount} optimization applied", style = MaterialTheme.typography.bodySmall, color = p.textPrimary)
        }
        r.checks.forEach {
            val mark = when (it.status) {
                RexCheckStatus.OK -> "•"
                RexCheckStatus.APPLIED -> "✓"
                RexCheckStatus.WARNING -> "⚠"
                RexCheckStatus.INFO -> "ℹ"
            }
            Text("$mark ${it.title}: ${it.detail}", style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
        }
        if (r.ramChangedSignificantly && r.ramBeforeAvailableBytes != null && r.ramAfterAvailableBytes != null) {
            Text(
                "RAM tersedia: ${RexFormat.bytes(r.ramBeforeAvailableBytes)} → ${RexFormat.bytes(r.ramAfterAvailableBytes)}",
                style = MaterialTheme.typography.bodySmall, color = p.textPrimary
            )
        }
        if (r.tempChangedSignificantly) {
            Text(
                "Suhu baterai: ${RexFormat.temperature(r.batteryTempBeforeC)} → ${RexFormat.temperature(r.batteryTempAfterC)}",
                style = MaterialTheme.typography.bodySmall, color = p.textPrimary
            )
        }
        if (r.noSignificantChange) {
            Text("No significant change detected.", style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
        }
        r.recommendations.forEach {
            Text("→ $it", style = MaterialTheme.typography.bodySmall, color = p.orange)
        }
    }
}
