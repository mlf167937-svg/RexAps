package com.rexaps.rexwarp

import com.rexaps.rexwarp.data.RexWarpUsageDao
import com.rexaps.rexwarp.data.RexWarpUsageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate

class RexWarpUsageRepository(private val dao: RexWarpUsageDao) {

    fun observeAll(): Flow<List<RexWarpDailyUsage>> = dao.observeAll().map { rows ->
        rows.mapNotNull { e ->
            runCatching { RexWarpDailyUsage(LocalDate.parse(e.date), e.downloadBytes, e.uploadBytes) }.getOrNull()
        }
    }

    suspend fun addTraffic(date: LocalDate, dl: Long, ul: Long) {
        if (dl < 0 || ul < 0 || (dl == 0L && ul == 0L)) return
        dao.addTraffic(date.toString(), dl, ul)
    }

    suspend fun deleteDay(date: LocalDate) = dao.deleteDate(date.toString())
    suspend fun deleteAll() = dao.deleteAll()

    suspend fun exportJson(): String {
        val arr = JSONArray()
        dao.getAll().forEach {
            arr.put(JSONObject().put("date", it.date).put("downloadBytes", it.downloadBytes).put("uploadBytes", it.uploadBytes))
        }
        return JSONObject().put("version", 1).put("module", "RexWARP").put("usage", arr).toString(2)
    }

    /** Tanggal pada file menimpa tanggal yang sama di database. Seluruh file ditolak jika ada entri tidak valid. */
    suspend fun importJson(text: String): Result<Int> = runCatching {
        val rows = try {
            val root = JSONObject(text)
            require(root.getInt("version") == 1 && root.getString("module") == "RexWARP") { "not-rexwarp" }
            val arr = root.getJSONArray("usage")
            require(arr.length() <= MAX_ROWS) { "too-many" }
            val byDate = LinkedHashMap<String, RexWarpUsageEntity>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val date = LocalDate.parse(o.getString("date")).toString()
                val dl = o.getLong("downloadBytes"); val ul = o.getLong("uploadBytes")
                require(dl >= 0 && ul >= 0) { "negative" }
                byDate[date] = RexWarpUsageEntity(date, dl, ul)
            }
            byDate.values.toList()
        } catch (e: JSONException) {
            throw IllegalArgumentException("This file is not a valid RexWARP usage export.")
        } catch (e: java.time.format.DateTimeParseException) {
            throw IllegalArgumentException("This file is not a valid RexWARP usage export.")
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("This file is not a valid RexWARP usage export.")
        }
        dao.upsertAll(rows)
        rows.size
    }

    private companion object { const val MAX_ROWS = 20_000 }
}