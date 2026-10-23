package com.rexaps.rexwarp.data

import androidx.room.ColumnInfo

/** SQL aggregate over successful tests only; null values mean no samples for that metric. */
data class RexWarpSpeedTestAggregate(
    @ColumnInfo(name = "testCount") val testCount: Int,
    @ColumnInfo(name = "avgDownloadMbps") val avgDownloadMbps: Double?,
    @ColumnInfo(name = "avgUploadMbps") val avgUploadMbps: Double?,
    @ColumnInfo(name = "avgLatencyMs") val avgLatencyMs: Double?,
    @ColumnInfo(name = "avgJitterMs") val avgJitterMs: Double?,
    @ColumnInfo(name = "maxDownloadMbps") val maxDownloadMbps: Double?,
    @ColumnInfo(name = "maxUploadMbps") val maxUploadMbps: Double?,
    @ColumnInfo(name = "minLatencyMs") val minLatencyMs: Double?
)
