package com.rexaps.rexwarp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RexWarpSpeedTestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: RexWarpSpeedTestEntity)

    @Query("SELECT * FROM speed_test_history ORDER BY completedAtEpochMs DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RexWarpSpeedTestEntity>>

    @Query("""
        SELECT COUNT(*) AS testCount,
               AVG(downloadMbps) AS avgDownloadMbps,
               AVG(uploadMbps) AS avgUploadMbps,
               AVG(latencyMs) AS avgLatencyMs,
               AVG(jitterMs) AS avgJitterMs,
               MAX(downloadMbps) AS maxDownloadMbps,
               MAX(uploadMbps) AS maxUploadMbps,
               MIN(latencyMs) AS minLatencyMs
        FROM speed_test_history
        WHERE isSuccessful = 1 AND completedAtEpochMs >= :sinceEpochMs
          AND (:vpnConnected IS NULL OR vpnConnected = :vpnConnected)
    """)
    fun observeAggregateSince(sinceEpochMs: Long, vpnConnected: Boolean?): Flow<RexWarpSpeedTestAggregate>

    @Query("DELETE FROM speed_test_history WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("DELETE FROM speed_test_history")
    suspend fun deleteAll(): Int
}
