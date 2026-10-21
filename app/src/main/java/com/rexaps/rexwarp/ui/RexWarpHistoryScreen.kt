package com.rexaps.rexwarp.ui

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import com.rexaps.rexwarp.RexWarpDailyUsage
import com.rexaps.rexwarp.RexWarpFormat
import com.rexaps.rexwarp.ui.components.RexWarpInfoRow
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal val rexWarpHistoryDateFmt: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@Composable
fun RexWarpHistoryScreen(rows: List<RexWarpDailyUsage>?, modifier: Modifier = Modifier) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var selected by remember { mutableStateOf<RexWarpDailyUsage?>(null) }
    if (landscape) RexWarpHistoryScreenLandscape(rows, { selected = it }, modifier)
    else RexWarpHistoryScreenPortrait(rows, { selected = it }, modifier)

    selected?.let { day ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(day.date.format(rexWarpHistoryDateFmt)) },
            text = {
                Column {
                    RexWarpInfoRow("Download", RexWarpFormat.bytes(day.downloadBytes))
                    RexWarpInfoRow("Upload", RexWarpFormat.bytes(day.uploadBytes))
                    RexWarpInfoRow("Total", RexWarpFormat.bytes(day.totalBytes))
                    if (day.totalBytes > 0) RexWarpInfoRow("Download share", "${day.downloadBytes * 100 / day.totalBytes}%")
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } }
        )
    }
}

@Composable
internal fun RexWarpHistoryRowCard(day: RexWarpDailyUsage, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Open details", onClick = onClick),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(day.date.format(rexWarpHistoryDateFmt), style = MaterialTheme.typography.titleSmall)
            Text(
                "↓ ${RexWarpFormat.bytes(day.downloadBytes)}   ↑ ${RexWarpFormat.bytes(day.uploadBytes)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("Total ${RexWarpFormat.bytes(day.totalBytes)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
