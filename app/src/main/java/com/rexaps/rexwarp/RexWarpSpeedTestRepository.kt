package com.rexaps.rexwarp

import com.rexaps.rexwarp.data.RexWarpSpeedTestAggregate
import com.rexaps.rexwarp.data.RexWarpSpeedTestDao
import com.rexaps.rexwarp.data.RexWarpSpeedTestEntity
import kotlinx.coroutines.flow.Flow

/** Persistence boundary for measured Cloudflare tests. It stores measured values, never UI placeholders. */
class RexWarpSpeedTestRepository(private val dao: RexWarpSpeedTestDao) {
    fun observeRecent(limit: Int = 50): Flow<List<RexWarpSpeedTestEntity>> =
        dao.observeRecent(limit.coerceIn(1, 200))

    fun observeAggregateSince(sinceEpochMs: Long, vpnConnected: Boolean?): Flow<RexWarpSpeedTestAggregate> =
        dao.observeAggregateSince(sinceEpochMs.coerceAtLeast(0L), vpnConnected)

    suspend fun save(result: RexWarpSpeedTestEntity) = dao.insert(result)
    suspend fun delete(id: String): Boolean = dao.delete(id) > 0
    suspend fun deleteAll(): Int = dao.deleteAll()
}
