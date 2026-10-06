package com.rexaps.rexmonitor.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rexaps.rexmonitor.RexFormat
import com.rexaps.rexmonitor.device.RexCpuInfo

@Composable
fun RexCpuCard(cpu: RexCpuInfo, is64Bit: Boolean, loading: Boolean, modifier: Modifier = Modifier) {
    val usage = cpu.usagePercent
    val freq = RexFormat.ghz(cpu.currentFreqKhz)
    val footers = buildList {
        add("${cpu.coreCount} Core • ${if (is64Bit) "64-bit" else "32-bit"}")
        cpu.model?.let { add(it) }
        if (usage == null) add("Penggunaan CPU: $REX_UNAVAILABLE (dibatasi Android/vendor)")
    }
    RexMetricCard(
        title = "CPU",
        icon = Icons.Rounded.DeveloperBoard,
        accent = RexTheme.palette.green,
        primary = freq,
        secondary = freq?.let { "GHz" },
        progress = usage?.div(100f),
        progressLabel = RexFormat.percent(usage),
        footers = footers,
        loading = loading,
        description = "CPU ${cpu.coreCount} core, frekuensi ${freq ?: REX_UNAVAILABLE}, penggunaan " +
            (usage?.let { "${it.toInt()} persen" } ?: REX_UNAVAILABLE),
        modifier = modifier
    )
}
