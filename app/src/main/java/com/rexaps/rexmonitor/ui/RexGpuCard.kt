package com.rexaps.rexmonitor.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rexaps.rexmonitor.device.RexGpuInfo

@Composable
fun RexGpuCard(gpu: RexGpuInfo, loading: Boolean, modifier: Modifier = Modifier) {
    val usage = gpu.usagePercent
    val api = listOfNotNull(gpu.glVersion, gpu.vulkanVersion?.let { "Vulkan $it" }).joinToString(" • ")
    RexMetricCard(
        title = "GPU",
        icon = Icons.Rounded.ViewInAr,
        accent = RexTheme.palette.magenta,
        primary = gpu.renderer,
        secondary = null,
        progress = usage?.div(100f),
        progressLabel = usage?.let { "$it%" },
        footers = buildList {
            if (api.isNotEmpty()) add(api)
            if (usage == null) add("Penggunaan GPU: $REX_UNAVAILABLE")
        },
        loading = loading,
        description = "GPU ${gpu.renderer ?: REX_UNAVAILABLE}",
        modifier = modifier
    )
}
