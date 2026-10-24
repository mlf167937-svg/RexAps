package com.rexaps.rexwarp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun CheckRow(check: DiagnosticCheck) {
    val colors = MaterialTheme.colorScheme
    val statusColor = when (check.status) {
        DiagnosticStatus.PASS -> colors.primary
        DiagnosticStatus.WARNING -> colors.tertiary
        DiagnosticStatus.FAIL -> colors.error
        DiagnosticStatus.NOT_TESTED -> colors.onSurfaceVariant
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(check.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Text(check.status.label, style = MaterialTheme.typography.labelSmall, color = statusColor)
            }
            Text(check.detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            check.durationMs?.let { Text("Waktu: ${it} ms", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant) }
        }
    }
}
