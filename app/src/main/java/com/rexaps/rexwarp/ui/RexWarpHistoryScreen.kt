package com.rexaps.rexwarp.ui

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import com.rexaps.rexwarp.DateRange
import com.rexaps.rexwarp.RexWarpBucket
import com.rexaps.rexwarp.RexWarpDailyUsage
import com.rexaps.rexwarp.RexWarpFormat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import com.rexaps.rexwarp.ui.components.RexWarpGraph
import com.rexaps.rexwarp.ui.components.RexWarpInfoRow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal val rexWarpHistoryDateFmt: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val rexWarpGraphLabelFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexWarpHistoryScreen(
    rows: List<RexWarpDailyUsage>?,
    modifier: Modifier = Modifier,
    graphRows: List<RexWarpDailyUsage>? = rows,
    hourlyRows: List<RexWarpBucket>? = null
) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var selected by remember { mutableStateOf<RexWarpDailyUsage?>(null) }
    var graphDays by rememberSaveable { mutableIntStateOf(30) } // -24 = hourly, 0 = custom
    var customRange by remember { mutableStateOf<DateRange?>(null) }
    var showCustomPicker by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val validCustom = customRange?.takeIf { !it.start.isAfter(it.end) && ChronoUnit.DAYS.between(it.start, it.end) in 0..365 }

    val buckets = remember(graphRows, hourlyRows, graphDays, validCustom, today) {
        when {
            graphDays == -24 -> hourlyRows.orEmpty()
            graphDays == 0 && validCustom == null -> emptyList()
            else -> {
                val range = validCustom?.takeIf { graphDays == 0 }
                    ?: DateRange(today.minusDays((graphDays - 1).coerceAtLeast(0).toLong()), today)
                val byDate = graphRows.orEmpty().associateBy { it.date }
                range.days(maxDays = 366).map { date ->
                    val usage = byDate[date]
                    val label = date.format(rexWarpGraphLabelFmt)
                    if (usage != null) RexWarpBucket(label, usage.downloadBytes, usage.uploadBytes, hasData = true)
                    else RexWarpBucket(label, 0L, 0L, hasData = false)
                }
            }
        }
    }
    val visibleRows = remember(rows, graphDays, validCustom, today) {
        rows?.filter { row ->
            when {
                graphDays == -24 -> row.date == today
                graphDays == 0 -> validCustom?.let { row.date in it } == true
                else -> !row.date.isBefore(today.minusDays((graphDays - 1).coerceAtLeast(0).toLong())) && !row.date.isAfter(today)
            }
        }
    }
    val onGraphDays: (Int) -> Unit = { choice ->
        graphDays = choice
        if (choice == 0) showCustomPicker = true
    }

    if (landscape) {
        RexWarpHistoryScreenLandscape(visibleRows, buckets, graphDays, onGraphDays, { selected = it }, modifier, loadingHourly = graphDays == -24 && hourlyRows == null, rangeLabel = validCustom?.let { "${it.start} – ${it.end}" })
    } else {
        RexWarpHistoryScreenPortrait(visibleRows, buckets, graphDays, onGraphDays, { selected = it }, modifier, loadingHourly = graphDays == -24 && hourlyRows == null, rangeLabel = validCustom?.let { "${it.start} – ${it.end}" })
    }

    if (showCustomPicker) {
        RexWarpHistoryDateRangePicker(
            initial = customRange,
            onDismiss = { showCustomPicker = false; if (customRange == null) graphDays = 30 },
            onApply = { range -> customRange = range; graphDays = 0; showCustomPicker = false }
        )
    }

    selected?.let { day ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(day.date.format(rexWarpHistoryDateFmt)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RexWarpInfoRow("Download", RexWarpFormat.bytes(day.downloadBytes))
                    RexWarpInfoRow("Upload", RexWarpFormat.bytes(day.uploadBytes))
                    RexWarpInfoRow("Total", RexWarpFormat.bytes(day.totalBytes))
                    if (day.totalBytes > 0) RexWarpInfoRow("Download share", "${day.downloadBytes * 100 / day.totalBytes}%")
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Tutup") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RexWarpHistoryDateRangePicker(
    initial: DateRange?,
    onDismiss: () -> Unit,
    onApply: (DateRange) -> Unit
) {
    val today = remember { LocalDate.now() }
    fun millis(date: LocalDate?): Long? = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    fun utcDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = millis(initial?.start),
        initialSelectedEndDateMillis = millis(initial?.end),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = !utcDate(utcTimeMillis).isAfter(today)
        }
    )
    val start = state.selectedStartDateMillis?.let(::utcDate)
    val end = state.selectedEndDateMillis?.let(::utcDate)
    val valid = start != null && end != null && !start.isAfter(end) && ChronoUnit.DAYS.between(start, end) in 0..365
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { if (valid) onApply(DateRange(start!!, end!!)) }, enabled = valid) { Text("Terapkan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Pilih rentang maksimum 366 hari", style = MaterialTheme.typography.titleSmall)
            DateRangePicker(state = state, modifier = Modifier.height(500.dp))
        }
    }
}

@Composable
internal fun RexWarpHistoryGraphHeader(days: Int, onDaysChange: (Int) -> Unit, rangeLabel: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Aktivitas historis", style = MaterialTheme.typography.titleLarge)
        Text("Grafik menggunakan agregat harian tersimpan atau data per menit untuk 24 jam. Interval tanpa sampel tidak dianggap sebagai nol.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(-24 to "24H", 7 to "7D", 30 to "30D", 90 to "90D", 0 to "Custom").forEach { (choice, label) ->
                FilterChip(selected = days == choice, onClick = { onDaysChange(choice) }, label = { Text(label) })
            }
        }
        if (days == 0 && rangeLabel != null) Text("Rentang: $rangeLabel", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun RexWarpHistoryRowCard(day: RexWarpDailyUsage, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val downloadShare = if (day.totalBytes > 0) day.downloadBytes.toFloat() / day.totalBytes.toFloat() else 0f
    ElevatedCard(
        modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Buka detail tanggal", onClick = onClick),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(day.date.format(rexWarpHistoryDateFmt), style = MaterialTheme.typography.titleSmall)
                Text(RexWarpFormat.bytes(day.totalBytes), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("↓ ${RexWarpFormat.bytes(day.downloadBytes)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Text("↑ ${RexWarpFormat.bytes(day.uploadBytes)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
            }
            LinearProgressIndicator(
                progress = downloadShare.coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = .65f)
            )
            Text(
                "Download ${(downloadShare * 100).toInt()}% · ketuk untuk detail",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
