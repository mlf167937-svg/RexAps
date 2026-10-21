package com.rexaps.rexwarp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.core.tween
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

private val landscapeDateFmt: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RexWarpUsageScreenLandscape(
    ui: RexWarpUsageUi?,
    onPreset: (RexWarpRangePreset) -> Unit,
    onGraph: (RexWarpGraphRange) -> Unit,
    onCustomRange: (DateRange) -> Unit,
    modifier: Modifier = Modifier
) {
    if (ui == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    var pickerFor by remember { mutableStateOf<String?>(null) }
    Row(
        modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            Modifier.weight(0.88f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            AnimatedVisibility(true, enter = fadeIn(tween(320)) + slideInHorizontally(tween(320)) { it / 18 }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Traffic overview", style = MaterialTheme.typography.titleLarge)
                    if (!ui.hasAnyData) RexWarpEmptyState(
                        "No usage data yet",
                        "Turn on usage recording or connect the tunnel. You can import a backup in Settings."
                    )
                    Text("Date range", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RexWarpRangePreset.entries.forEach { preset ->
                            FilterChip(
                                selected = ui.selection.preset == preset,
                                label = { Text(preset.label) },
                                onClick = {
                                    if (preset == RexWarpRangePreset.CUSTOM) pickerFor = "usage"
                                    onPreset(preset)
                                }
                            )
                        }
                    }
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(15.dp)) {
                            Text(
                                ui.range?.let { "${it.start.format(landscapeDateFmt)} → ${it.end.format(landscapeDateFmt)}" } ?: "Choose a custom range",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            RexWarpInfoRow("Download", RexWarpFormat.bytes(ui.rangeSummary.downloadBytes))
                            RexWarpInfoRow("Upload", RexWarpFormat.bytes(ui.rangeSummary.uploadBytes))
                            RexWarpInfoRow("Total", RexWarpFormat.bytes(ui.rangeSummary.totalBytes))
                        }
                    }
                    RexWarpBackupCard()
                }
            }
        }
        Column(
            Modifier.weight(1.32f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            AnimatedVisibility(true, enter = fadeIn(tween(400, delayMillis = 70)) + slideInHorizontally(tween(400, delayMillis = 70)) { it / 18 }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Traffic graph", style = MaterialTheme.typography.titleLarge)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RexWarpGraphRange.entries.forEach { range ->
                            FilterChip(
                                selected = ui.selection.graph == range,
                                label = { Text(range.label) },
                                onClick = {
                                    if (range == RexWarpGraphRange.CUSTOM) pickerFor = "graph"
                                    onGraph(range)
                                }
                            )
                        }
                    }
                    when {
                        !ui.hasAnyData -> Unit
                        ui.graphBuckets.isEmpty() -> RexWarpEmptyState("Pick a custom range", "Select start and end dates to draw the graph.")
                        !ui.hasDataInGraphRange -> RexWarpEmptyState("No usage data in this range", "Try a longer range.")
                        else -> ElevatedCard(Modifier.fillMaxWidth()) {
                            RexWarpGraph(ui.graphBuckets, Modifier.fillMaxWidth().padding(16.dp))
                        }
                    }
                }
            }
        }
    }

    pickerFor?.let {
        val today = remember { LocalDate.now() }
        val state = rememberDateRangePickerState(selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = !landscapeUtcDate(utcTimeMillis).isAfter(today)
        })
        DatePickerDialog(
            onDismissRequest = { pickerFor = null },
            confirmButton = {
                TextButton(
                    enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null,
                    onClick = {
                        onCustomRange(DateRange(landscapeUtcDate(state.selectedStartDateMillis!!), landscapeUtcDate(state.selectedEndDateMillis!!)))
                        pickerFor = null
                    }
                ) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { pickerFor = null }) { Text("Cancel") } }
        ) { DateRangePicker(state = state, modifier = Modifier.height(500.dp)) }
    }
}

private fun landscapeUtcDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
