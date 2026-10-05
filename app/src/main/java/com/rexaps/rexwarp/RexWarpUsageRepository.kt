package com.rexaps.rexwarp

import com.rexaps.rexwarp.data.RexWarpUsageDao
import com.rexaps.rexwarp.data.RexWarpUsageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class RexWarpUsageRepository(
    private val dao: RexWarpUsageDao,
    private val backup: RexWarpBackupStore
) {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun observeAll(): Flow<List<RexWarpDailyUsage>> = dao.observeAll().map { rows ->
        rows.mapNotNull { e ->
            runCatching { RexWarpDailyUsage(LocalDate.parse(e.date), e.downloadBytes, e.uploadBytes) }.getOrNull()
        }
    }

    /** Grafik per jam untuk [hours] jam terakhir (jam berjalan = bucket terakhir). */
    fun observeHours(hours: Int): Flow<List<RexWarpBucket>> {
        val startHour = ZonedDateTime.now(zone).truncatedTo(ChronoUnit.HOURS).minusHours((hours - 1).toLong())
        val fmt = DateTimeFormatter.ofPattern("HH:mm")
        return dao.observeMinutesFrom(startHour.toEpochSecond() / 60).map { rows ->
            val start = ZonedDateTime.now(zone).truncatedTo(ChronoUnit.HOURS).minusHours((hours - 1).toLong())
            val from = start.toEpochSecond() / 60
            val dl = LongArray(hours); val ul = LongArray(hours)
            rows.forEach { r ->
                val idx = ((r.minute - from) / 60).toInt()
                if (r.minute >= from && idx in 0 until hours) { dl[idx] += r.downloadBytes; ul[idx] += r.uploadBytes }
            }
            List(hours) { i -> RexWarpBucket(start.plusHours(i.toLong()).format(fmt), dl[i], ul[i]) }
        }
    }

    /** Dinonaktifkan: pencatatan sekarang dilakukan RexWarpRecorderService lewat [record]. Dibiarkan agar RexWarpVpnService tidak mencatat ganda. */
    @Suppress("UNUSED_PARAMETER")
    suspend fun addTraffic(date: LocalDate, dl: Long, ul: Long) = Unit

    /** Mencatat satu menit ke database (menit + total harian) lalu menambahkannya ke CSV. */
    suspend fun record(minute: Long, dl: Long, ul: Long, warp: Boolean) {
        if (dl < 0 || ul < 0 || (dl == 0L && ul == 0L)) return
        val time = LocalDateTime.ofInstant(Instant.ofEpochSecond(minute * 60), zone)
        val date = time.toLocalDate()
        val w = if (warp) 1 else 0
        dao.addMinute(minute, dl, ul, w, date.toString())
        withContext(Dispatchers.IO) { backup.append(date, "$time,$dl,$ul,$w") }
    }

    suspend fun pruneMinutesOlderThan(days: Long) {
        val before = Instant.now().minus(days, ChronoUnit.DAYS).epochSecond / 60
        dao.deleteMinutesBefore(before)
    }

    suspend fun deleteDay(date: LocalDate) {
        val from = date.atStartOfDay(zone).toEpochSecond() / 60
        val to = date.plusDays(1).atStartOfDay(zone).toEpochSecond() / 60 - 1
        dao.deleteDate(date.toString())
        dao.deleteMinutes(from, to)
        withContext(Dispatchers.IO) { backup.deleteDay(date) }
    }

    suspend fun deleteAll() {
        dao.deleteAll()
        dao.deleteAllMinutes()
        withContext(Dispatchers.IO) { backup.deleteAll() }
    }

    /** Memulihkan data dari CSV di folder backup. Tanggal yang sama ditimpa. */
    suspend fun restoreFromFolder(): Result<Int> = runCatching {
        val rows = withContext(Dispatchers.IO) { backup.readAll() }
        require(rows.isNotEmpty()) { "No usage files found in the selected folder." }
        val daily = rows.groupBy { LocalDateTime.ofInstant(Instant.ofEpochSecond(it.minute * 60), zone).toLocalDate() }
            .map { (d, list) -> RexWarpUsageEntity(d.toString(), list.sumOf { it.downloadBytes }, list.sumOf { it.uploadBytes }) }
        dao.upsertMinutes(rows)
        dao.upsertAll(daily)
        daily.size
    }

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