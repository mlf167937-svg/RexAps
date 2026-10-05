package com.rexaps.rexwarp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.RexWarpFormat
import com.rexaps.rexwarp.RexWarpPeriodSummaries
import com.rexaps.rexwarp.model.RexWarpUsageSummary

@Composable
fun RexWarpUsageCard(title: String, summary: RexWarpUsageSummary, modifier: Modifier = Modifier) {
    val dl = RexWarpFormat.bytes(summary.downloadBytes); val ul = RexWarpFormat.bytes(summary.uploadBytes)
    val total = RexWarpFormat.bytes(summary.totalBytes)
    Card(modifier.semantics(mergeDescendants = true) { contentDescription = "$title. Download $dl. Upload $ul. Total $total." }) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("↓ $dl", style = MaterialTheme.typography.bodyMedium)
            Text("↑ $ul", style = MaterialTheme.typography.bodyMedium)
            Text("Total $total", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun RexWarpTrafficSection(periods: RexWarpPeriodSummaries?, hasUsage: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Traffic usage", style = MaterialTheme.typography.titleMedium)
        when {
            periods == null -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            !hasUsage -> RexWarpEmptyState("No usage data yet", "Traffic is recorded while the RexWARP tunnel is connected.")
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RexWarpUsageCard("Today", periods.today, Modifier.weight(1f))
                    RexWarpUsageCard("This week", periods.week, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RexWarpUsageCard("This month", periods.month, Modifier.weight(1f))
                    RexWarpUsageCard("All time", periods.allTime, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun RexWarpEmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
