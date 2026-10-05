package com.rexaps.rexwarp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RexWarpUsageDao {

    // ---- harian ----
    @Query("SELECT * FROM usage_daily ORDER BY date ASC")
    abstract fun observeAll(): Flow<List<RexWarpUsageEntity>>

    @Query("SELECT * FROM usage_daily ORDER BY date ASC")
    abstract suspend fun getAll(): List<RexWarpUsageEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIgnore(e: RexWarpUsageEntity): Long

    @Query("UPDATE usage_daily SET downloadBytes = downloadBytes + :dl, uploadBytes = uploadBytes + :ul WHERE date = :date")
    abstract suspend fun increment(date: String, dl: Long, ul: Long): Int

    @Transaction
    open suspend fun addTraffic(date: String, dl: Long, ul: Long) {
        if (increment(date, dl, ul) == 0) insertIgnore(RexWarpUsageEntity(date, dl, ul))
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertAll(rows: List<RexWarpUsageEntity>)

    @Query("DELETE FROM usage_daily WHERE date = :date")
    abstract suspend fun deleteDate(date: String)

    @Query("DELETE FROM usage_daily")
    abstract suspend fun deleteAll()

    // ---- per menit ----
    @Query("SELECT * FROM usage_minute WHERE minute >= :from ORDER BY minute ASC")
    abstract fun observeMinutesFrom(from: Long): Flow<List<RexWarpMinuteEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertMinuteIgnore(e: RexWarpMinuteEntity): Long

    @Query("UPDATE usage_minute SET downloadBytes = downloadBytes + :dl, uploadBytes = uploadBytes + :ul, warp = MAX(warp, :warp) WHERE minute = :minute")
    abstract suspend fun incrementMinute(minute: Long, dl: Long, ul: Long, warp: Int): Int

    /** Menambah baris menit sekaligus total hariannya dalam satu transaksi. */
    @Transaction
    open suspend fun addMinute(minute: Long, dl: Long, ul: Long, warp: Int, date: String) {
        if (incrementMinute(minute, dl, ul, warp) == 0) insertMinuteIgnore(RexWarpMinuteEntity(minute, dl, ul, warp))
        addTraffic(date, dl, ul)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertMinutes(rows: List<RexWarpMinuteEntity>)

    @Query("DELETE FROM usage_minute WHERE minute BETWEEN :from AND :to")
    abstract suspend fun deleteMinutes(from: Long, to: Long)

    @Query("DELETE FROM usage_minute WHERE minute < :before")
    abstract suspend fun deleteMinutesBefore(before: Long)

    @Query("DELETE FROM usage_minute")
    abstract suspend fun deleteAllMinutes()
}