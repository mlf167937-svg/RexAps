package com.rexaps.rexmonitor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rexaps.rexmonitor.RexFormat
import com.rexaps.rexmonitor.device.RexBatteryInfo
import com.rexaps.rexmonitor.device.RexChargeType

@Composable
fun RexBatteryCard(battery: RexBatteryInfo, loading: Boolean, modifier: Modifier = Modifier) {
    val p = RexTheme.palette
    val charging = battery.isCharging == true
    val statusText = when (battery.isCharging) {
        true -> "Mengisi daya" + when (battery.chargeType) {
            RexChargeType.AC -> " (AC)"
            RexChargeType.USB -> " (USB)"
            RexChargeType.WIRELESS -> " (Nirkabel)"
            else -> ""
        }
        false -> "Tidak mengisi daya"
        null -> REX_UNAVAILABLE
    }
    val desc = "Baterai ${battery.percent?.let { "$it persen" } ?: REX_UNAVAILABLE}, $statusText"
    RexCard(modifier, p.green, desc) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RexIconBadge(if (charging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull, p.green)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Baterai", style = MaterialTheme.typography.labelLarge, color = p.textSecondary)
                Text(
                    if (loading) "Memuat…" else battery.percent?.let { "$it%" } ?: REX_UNAVAILABLE,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = p.textPrimary
                )
            }
        }
        if (!loading) {
            Spacer(Modifier.height(10.dp))
            RexProgress(battery.percent?.div(100f), p.green, Modifier)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                RexLabelValue("Status", statusText)
                battery.chargeRemainingMs?.let {
                    val min = it / 60_000
                    RexLabelValue("Penuh dalam", "${min / 60}j ${min % 60}m")
                }
                RexLabelValue("Kesehatan", battery.health ?: REX_UNAVAILABLE)
                RexLabelValue("Suhu", RexFormat.temperature(battery.temperatureC) ?: REX_UNAVAILABLE)
                RexLabelValue("Tegangan", battery.voltageMv?.let { "$it mV" } ?: REX_UNAVAILABLE)
                RexLabelValue("Arus", battery.currentMa?.let { "$it mA" } ?: REX_UNAVAILABLE)
            }
        }
    }
}
