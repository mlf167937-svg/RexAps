package com.rexaps.rexwarp.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.*
import com.rexaps.rexwarp.ui.components.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexWarpUsageScreen(
    ui: RexWarpUsageUi?, onPreset: (RexWarpRangePreset) -> Unit, onGraph: (RexWarpGraphRange) -> Unit,
    onCustomRange: (DateRange) -> Unit, modifier: Modifier = Modifier
) {
    if (ui == null) { Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    var pickerFor by remember { mutableStateOf<String?>(null) } // "usage" | "graph"

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!ui.hasAnyData) {
            RexWarpEmptyState("No usage data yet", "Turn on usage recording below, or connect the tunnel. You can also import a RexWARP export in Settings.")
        }

        Text("Date range", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.chipRow(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RexWarpRangePreset.entries.forEach { p ->
                FilterChip(selected = ui.selection.preset == p, label = { Text(p.label) }, onClick = {
                    if (p == RexWarpRangePreset.CUSTOM) pickerFor = "usage"
                    onPreset(p)
                })
            }
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(ui.range?.let { "${it.start.format(dateFmt)} → ${it.end.format(dateFmt)}" } ?: "Choose a custom range",
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                RexWarpInfoRow("Download", RexWarpFormat.bytes(ui.rangeSummary.downloadBytes))
                RexWarpInfoRow("Upload", RexWarpFormat.bytes(ui.rangeSummary.uploadBytes))
                RexWarpInfoRow("Total", RexWarpFormat.bytes(ui.rangeSummary.totalBytes))
            }
        }

        Text("Traffic graph", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.chipRow(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RexWarpGraphRange.entries.forEach { g ->
                FilterChip(selected = ui.selection.graph == g, label = { Text(g.label) }, onClick = {
                    if (g == RexWarpGraphRange.CUSTOM) pickerFor = "graph"
                    onGraph(g)
                })
            }
        }
        when {
            !ui.hasAnyData -> Unit
            ui.graphBuckets.isEmpty() -> RexWarpEmptyState("Pick a custom range", "Select start and end dates to draw the graph.")
            !ui.hasDataInGraphRange -> RexWarpEmptyState("No usage data in this range", "Try a longer range.")
            else -> ElevatedCard(Modifier.fillMaxWidth()) { RexWarpGraph(ui.graphBuckets, Modifier.padding(16.dp)) }
        }

        RexWarpBackupCard()
    }

    pickerFor?.let {
        val today = remember { LocalDate.now() }
        val state = rememberDateRangePickerState(selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = !utcDate(utcTimeMillis).isAfter(today)
        })
        DatePickerDialog(
            onDismissRequest = { pickerFor = null },
            confirmButton = {
                TextButton(enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null, onClick = {
                    onCustomRange(DateRange(utcDate(state.selectedStartDateMillis!!), utcDate(state.selectedEndDateMillis!!)))
                    pickerFor = null
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { pickerFor = null }) { Text("Cancel") } }
        ) { DateRangePicker(state = state, modifier = Modifier.height(500.dp)) }
    }
}

private fun utcDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun Modifier.chipRow(): Modifier = this.fillMaxWidth().horizontalScroll(rememberScrollState())
