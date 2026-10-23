package com.rexaps.rexwarp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Persisted measurement. Nullable metric fields mean the engine did not provide that metric. */
@Entity(
    tableName = "speed_test_history",
    indices = [Index(value = ["completedAtEpochMs"])]
)
data class RexWarpSpeedTestEntity(
    @PrimaryKey val id: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long,
    val downloadMbps: Double?,
    val uploadMbps: Double?,
    val latencyMs: Double?,
    val jitterMs: Double?,
    val durationMs: Long?,
    val vpnConnected: Boolean?,
    val isSuccessful: Boolean,
    val errorSummary: String?,
    val source: String
)
