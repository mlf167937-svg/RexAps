package com.rexaps.rexmonitor.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rexaps.rexmonitor.RexFormat
import com.rexaps.rexmonitor.device.RexStorageInfo

@Composable
fun RexStorageCard(storage: RexStorageInfo, loading: Boolean, modifier: Modifier = Modifier) {
    val used = storage.usedBytes
    val total = storage.totalBytes
    val pct = storage.usedPercent
    RexMetricCard(
        title = "Storage",
        icon = Icons.Rounded.Storage,
        accent = RexTheme.palette.purple,
        primary = used?.let { RexFormat.gbValue(it) },
        secondary = total?.let { "/ ${RexFormat.gbValue(it)} GB" },
        progress = pct?.div(100f),
        progressLabel = RexFormat.percent(pct),
        footers = listOfNotNull(storage.freeBytes?.let { "${RexFormat.gbValue(it)} GB tersedia" }),
        loading = loading,
        description = if (used != null && total != null)
            "Penyimpanan terpakai ${RexFormat.gbValue(used)} dari ${RexFormat.gbValue(total)} gigabyte, ${pct?.toInt()} persen"
        else "Penyimpanan $REX_UNAVAILABLE",
        modifier = modifier
    )
}
