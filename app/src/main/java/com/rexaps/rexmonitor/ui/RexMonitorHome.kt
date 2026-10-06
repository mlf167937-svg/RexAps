package com.rexaps.rexmonitor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.rexaps.rexmonitor.RexHealthLevel
import com.rexaps.rexmonitor.RexMonitorState
import kotlinx.coroutines.delay

@Composable
fun RexMonitorHome(
    state: RexMonitorState,
    onToggleMonitoring: (Boolean) -> Unit,
    onOptimize: () -> Unit,
    onRequestRoot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = RexTheme.palette
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .then(Modifier)
    ) {
        val wide = maxWidth >= 840.dp
        val metricColumns = if (wide) 4 else 2
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 1200.dp).fillMaxWidth()) {
                RexHeader(state)
                Spacer(Modifier.height(12.dp))

                val loading = state.loading
                RexGrid(
                    columns = metricColumns,
                    items = listOf<@Composable (Modifier) -> Unit>(
                        { m -> RexRamCard(state.ram, loading, m) },
                        { m -> RexStorageCard(state.storage, loading, m) },
                        { m -> RexCpuCard(state.cpu, state.device.is64Bit, loading, m) },
                        { m -> RexGpuCard(state.gpu, loading, m) }
                    )
                )
                Text(
                    "Android menggunakan sebagian RAM untuk cache dan sistem agar aplikasi dapat berjalan lebih cepat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textSecondary
                )
                Spacer(Modifier.height(12.dp))

                RexGrid(
                    columns = if (wide) 2 else 1,
                    items = listOf<@Composable (Modifier) -> Unit>(
                        { m -> RexTemperatureCard(state.temperature, loading, m) },
                        { m -> RexNetworkCard(state.network, loading, m) }
                    )
                )
                RexGrid(
                    columns = if (wide) 2 else 1,
                    items = listOf<@Composable (Modifier) -> Unit>(
                        { m -> RexBatteryCard(state.battery, loading, m) },
                        { m -> RexSystemCard(state.device, state.system, state.network, state.temperature, loading, m) }
                    )
                )
                RexGrid(
                    columns = if (wide) 2 else 1,
                    items = listOf<@Composable (Modifier) -> Unit>(
                        { m -> RexNotificationCard(state, onToggleMonitoring, m) },
                        { m ->
                            RexBoosterCard(
                                health = state.health,
                                booster = state.booster,
                                rootAvailable = state.rootAvailable,
                                rootGranted = state.rootGranted,
                                onOptimize = onOptimize,
                                onRequestRoot = onRequestRoot,
                                modifier = m
                            )
                        }
                    )
                )

                FreshnessText(state.lastUpdatedMs)
                if (state.errors.isNotEmpty()) {
                    Text(
                        "Sebagian data gagal dibaca: ${state.errors.joinToString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.orange
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RexGrid(columns: Int, items: List<@Composable (Modifier) -> Unit>) {
    items.chunked(columns).forEach { rowItems ->
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rowItems.forEach { item -> item(Modifier.weight(1f).fillMaxHeight()) }
            repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun RexHeader(state: RexMonitorState) {
    val p = RexTheme.palette
    val (icon, tint) = when (state.health.level) {
        RexHealthLevel.NORMAL -> Icons.Rounded.CheckCircle to p.green
        RexHealthLevel.WARM -> Icons.Rounded.Warning to p.orange
        RexHealthLevel.ATTENTION -> Icons.Rounded.Error to p.red
    }
    RexCard(Modifier.fillMaxWidth(), p.blue) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = p.textPrimary)) { append("Rex") }
                        withStyle(SpanStyle(color = p.cyan)) { append("MONITOR") }
                        withStyle(SpanStyle(color = p.purple, fontSize = MaterialTheme.typography.labelMedium.fontSize)) {
                            append("  PRO")
                        }
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.semantics { heading() }
                )
                Text("Pantau Device • Performa Maksimal", style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Ikon + teks, tidak hanya warna.
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.width(6.dp))
            Text(state.health.label, style = MaterialTheme.typography.titleSmall, color = tint, fontWeight = FontWeight.SemiBold)
        }
        state.health.reasons.forEach {
            Text("• $it", style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
        }
        Spacer(Modifier.height(6.dp))
        val d = state.device
        Text(
            d.displayName ?: REX_UNAVAILABLE,
            style = MaterialTheme.typography.titleSmall,
            color = p.textPrimary
        )
        Text(
            listOfNotNull(
                d.androidVersion?.let { "Android $it" },
                "API ${d.sdkInt}",
                if (d.is64Bit) "64-bit" else null,
                state.battery.percent?.let { "Baterai $it%" + if (state.battery.isCharging == true) " ⚡" else "" }
            ).joinToString(" • "),
            style = MaterialTheme.typography.bodySmall,
            color = p.textSecondary
        )
    }
}

@Composable
private fun FreshnessText(lastUpdatedMs: Long?) {
    val p = RexTheme.palette
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1_000)
            value = System.currentTimeMillis()
        }
    }
    val text = if (lastUpdatedMs == null) "Memuat data…" else {
        val age = ((now - lastUpdatedMs) / 1000).coerceAtLeast(0)
        when {
            age < 2 -> "Diperbarui baru saja"
            age <= 10 -> "Diperbarui $age dtk lalu"
            else -> "Data terakhir diketahui ($age dtk lalu)"
        }
    }
    Text(text, style = MaterialTheme.typography.labelSmall, color = p.textSecondary)
}
