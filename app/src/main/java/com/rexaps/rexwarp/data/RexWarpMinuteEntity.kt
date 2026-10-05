package com.rexaps.rexwarp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** minute = epoch menit (epochSeconds / 60). warp: 1 jika menit itu diukur lewat tunnel WARP. */
@Entity(tableName = "usage_minute")
data class RexWarpMinuteEntity(
    @PrimaryKey val minute: Long,
    val downloadBytes: Long,
    val uploadBytes: Long,
    val warp: Int
)
