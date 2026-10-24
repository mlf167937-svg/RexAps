package com.rexaps.rexwarp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ComparisonMetric(label: String, a: Double?, b: Double?, unit: String) {
    val delta = if (a != null && b != null) b - a else null
    val percent = if (a != null && b != null && a > 0.0) (b - a) * 100.0 / a else null
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text("${formatNumber(a)} → ${formatNumber(b)} $unit", style = MaterialTheme.typography.bodyMedium)
        Text(
            delta?.let { " (${if (it > 0) "+" else ""}${formatNumber(it)}${percent?.let { p -> ", ${if (p > 0) "+" else ""}${formatNumber(p)}%" }.orEmpty()})" } ?: " —",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
internal fun SmallMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1)
    }
}
