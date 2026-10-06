package com.rexaps.rexmonitor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rexaps.rexmonitor.RexFormat
import com.rexaps.rexmonitor.device.RexTemperatureInfo
import com.rexaps.rexmonitor.device.RexThermalLevel

fun thermalLabel(level: RexThermalLevel?): String = when (level) {
    null -> REX_UNAVAILABLE
    RexThermalLevel.NONE -> "Normal"
    RexThermalLevel.LIGHT -> "Hangat ringan"
    RexThermalLevel.MODERATE -> "Sedang"
    RexThermalLevel.SEVERE -> "Tinggi"
    RexThermalLevel.CRITICAL -> "Kritis"
    RexThermalLevel.EMERGENCY -> "Darurat"
    RexThermalLevel.SHUTDOWN -> "Akan mati"
}

@Composable
fun RexTemperatureCard(temp: RexTemperatureInfo, loading: Boolean, modifier: Modifier = Modifier) {
    val p = RexTheme.palette
    val desc = "Suhu perangkat. CPU ${RexFormat.temperature(temp.cpuC) ?: REX_UNAVAILABLE}, " +
        "GPU ${RexFormat.temperature(temp.gpuC) ?: REX_UNAVAILABLE}, " +
        "baterai ${RexFormat.temperature(temp.batteryC) ?: REX_UNAVAILABLE}"
    RexCard(modifier, p.blue, desc) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RexIconBadge(Icons.Rounded.Thermostat, p.blue)
            Spacer(Modifier.width(10.dp))
            Text("Suhu Device", style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TempColumn("CPU", temp.cpuC, temp.cpuHistory, p.blue, loading, Modifier.weight(1f))
            TempColumn("GPU", temp.gpuC, temp.gpuHistory, p.magenta, loading, Modifier.weight(1f))
            TempColumn("Baterai", temp.batteryC, temp.batteryHistory, p.green, loading, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        RexLabelValue("Status termal Android", if (loading) "…" else thermalLabel(temp.thermalLevel))
        if (!loading && (temp.cpuC == null || temp.gpuC == null)) {
            Text(
                "Suhu CPU/GPU hanya tampil bila sensor termal sistem dapat dibaca; Android tidak selalu menyediakannya.",
                style = MaterialTheme.typography.bodySmall,
                color = p.textSecondary
            )
        }
    }
}

@Composable
private fun TempColumn(
    label: String,
    value: Float?,
    history: List<Float>,
    color: Color,
    loading: Boolean,
    modifier: Modifier
) {
    val p = RexTheme.palette
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
        Text(
            if (loading) "…" else RexFormat.temperature(value) ?: REX_UNAVAILABLE,
            style = if (value != null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (value != null) p.textPrimary else p.textSecondary
        )
        RexSparkline(history, color, Modifier.fillMaxWidth().height(32.dp))
    }
}
