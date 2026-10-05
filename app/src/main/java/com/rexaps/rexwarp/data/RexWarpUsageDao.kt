package com.rexaps.rexwarp.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RexWarpUsageDao {
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
}