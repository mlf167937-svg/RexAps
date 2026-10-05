package com.rexaps.rexwarp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.RexWarpDailyUsage
import com.rexaps.rexwarp.RexWarpFormat
import com.rexaps.rexwarp.ui.components.RexWarpEmptyState
import com.rexaps.rexwarp.ui.components.RexWarpInfoRow
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun RexWarpHistoryScreen(rows: List<RexWarpDailyUsage>?, modifier: Modifier = Modifier) {
    val fmt = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    var selected by remember { mutableStateOf<RexWarpDailyUsage?>(null) }

    when {
        rows == null -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        rows.isEmpty() -> RexWarpEmptyState("No usage data yet", "Daily totals will appear here after the tunnel has carried traffic.", modifier.padding(16.dp))
        else -> LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows, key = { it.date.toString() }) { d ->
                Card(Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Open details") { selected = d }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(d.date.format(fmt), style = MaterialTheme.typography.titleSmall)
                        Text("↓ ${RexWarpFormat.bytes(d.downloadBytes)}   ↑ ${RexWarpFormat.bytes(d.uploadBytes)}", style = MaterialTheme.typography.bodyMedium)
                        Text("Total ${RexWarpFormat.bytes(d.totalBytes)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    selected?.let { d ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(d.date.format(fmt)) },
            text = {
                Column {
                    RexWarpInfoRow("Download", RexWarpFormat.bytes(d.downloadBytes))
                    RexWarpInfoRow("Upload", RexWarpFormat.bytes(d.uploadBytes))
                    RexWarpInfoRow("Total", RexWarpFormat.bytes(d.totalBytes))
                    if (d.totalBytes > 0) RexWarpInfoRow("Download share", "${d.downloadBytes * 100 / d.totalBytes}%")
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } }
        )
    }
}
