package com.rexaps.rexmonitor.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rexaps.rexmonitor.RexFormat
import com.rexaps.rexmonitor.device.RexRamInfo

@Composable
fun RexRamCard(ram: RexRamInfo, loading: Boolean, modifier: Modifier = Modifier) {
    val used = ram.usedBytes
    val total = ram.totalBytes
    val pct = ram.usedPercent
    RexMetricCard(
        title = "RAM",
        icon = Icons.Rounded.Memory,
        accent = RexTheme.palette.cyan,
        primary = used?.let { RexFormat.gbValue(it) },
        secondary = total?.let { "/ ${RexFormat.gbValue(it)} GB" },
        progress = pct?.div(100f),
        progressLabel = RexFormat.percent(pct),
        footers = listOfNotNull(
            ram.availableBytes?.let { "${RexFormat.gbValue(it)} GB tersedia" },
            if (ram.lowMemory) "Memori rendah" else null
        ),
        loading = loading,
        description = if (used != null && total != null)
            "RAM terpakai ${RexFormat.gbValue(used)} dari ${RexFormat.gbValue(total)} gigabyte, ${pct?.toInt()} persen"
        else "RAM $REX_UNAVAILABLE",
        modifier = modifier
    )
}
