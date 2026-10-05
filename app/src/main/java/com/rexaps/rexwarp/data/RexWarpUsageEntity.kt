package com.rexaps.rexwarp.data
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_daily")
data class RexWarpUsageEntity(
    @PrimaryKey val date: String,
    val downloadBytes: Long,
    val uploadBytes: Long
)
