package com.rexaps.rexwarp

import com.rexaps.rexwarp.model.RexWarpUsageSummary
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class RexWarpDailyUsage(val date: LocalDate, val downloadBytes: Long, val uploadBytes: Long) {
    val totalBytes: Long get() = downloadBytes + uploadBytes
}

data class DateRange(val start: LocalDate, val end: LocalDate) {
    operator fun contains(d: LocalDate) = !d.isBefore(start) && !d.isAfter(end)

    /** Maksimal [maxDays] hari terakhir yang berakhir di [end]. */
    fun days(maxDays: Int = 366): List<LocalDate> {
        val n = (ChronoUnit.DAYS.between(start, end) + 1).toInt().coerceIn(1, maxDays)
        return List(n) { end.minusDays((n - 1 - it).toLong()) }
    }
}

enum class RexWarpRangePreset(val label: String) {
    TODAY("Today"), YESTERDAY("Yesterday"), LAST_7_DAYS("Last 7 days"), LAST_30_DAYS("Last 30 days"),
    THIS_MONTH("This month"), LAST_MONTH("Last month"), CUSTOM("Custom range");

    fun resolve(today: LocalDate, custom: DateRange?): DateRange? = when (this) {
        TODAY -> DateRange(today, today)
        YESTERDAY -> today.minusDays(1).let { DateRange(it, it) }
        LAST_7_DAYS -> DateRange(today.minusDays(6), today)
        LAST_30_DAYS -> DateRange(today.minusDays(29), today)
        THIS_MONTH -> DateRange(today.withDayOfMonth(1), today)
        LAST_MONTH -> today.minusMonths(1).let { DateRange(it.withDayOfMonth(1), it.withDayOfMonth(it.lengthOfMonth())) }
        CUSTOM -> custom
    }
}

enum class RexWarpGraphRange(val label: String, val days: Int?) {
    DAYS_7("7 days", 7), DAYS_30("30 days", 30), DAYS_90("90 days", 90), CUSTOM("Custom", null)
}

data class RexWarpSelection(
    val preset: RexWarpRangePreset = RexWarpRangePreset.LAST_7_DAYS,
    val graph: RexWarpGraphRange = RexWarpGraphRange.DAYS_7,
    val custom: DateRange? = null
)

data class RexWarpPeriodSummaries(
    val today: RexWarpUsageSummary,
    val week: RexWarpUsageSummary,
    val month: RexWarpUsageSummary,
    val allTime: RexWarpUsageSummary
)

data class RexWarpUsageUi(
    val selection: RexWarpSelection,
    val range: DateRange?,
    val rangeSummary: RexWarpUsageSummary,
    val graphSeries: List<RexWarpDailyUsage>,
    val hasAnyData: Boolean,
    val hasDataInGraphRange: Boolean
)