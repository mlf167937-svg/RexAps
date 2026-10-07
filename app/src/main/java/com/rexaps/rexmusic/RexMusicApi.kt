package com.rexaps.rexmusic

import com.rexaps.rextools.utils.ApiClient
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

object RexMusicApi {

    private const val LYRICS_ENDPOINT = "https://api.nexray.eu.cc/search/lyrics"
    private const val SEARCH_TTL_MS = 5 * 60_000L
    private const val AUDIO_TTL_MS = 15 * 60_000L
    private const val LYRICS_TTL_MS = 24 * 60 * 60_000L
    private const val MAX_SEARCH_CACHE = 30
    private const val MAX_LYRICS_CACHE = 40

    private class Cached<T>(val value: T, val at: Long = System.currentTimeMillis()) {
        fun fresh(ttl: Long) = System.currentTimeMillis() - at < ttl
    }

    private class CachedLyrics(val value: Lyrics, val at: Long = System.currentTimeMillis()) {
        fun fresh() = System.currentTimeMillis() - at < LYRICS_TTL_MS
    }

    /** LRU: query -> hasil search. Akses selalu lewat synchronized. */
    private val searchCache =
        object : LinkedHashMap<String, Cached<List<RexTrack>>>(32, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, Cached<List<RexTrack>>>?
            ) = size > MAX_SEARCH_CACHE
        }

    /** spotifyUrl -> audioUrl (link audio biasanya expire, makanya ada TTL). */
    private val audioCache = HashMap<String, Cached<String>>()

    /** query -> lirik (TTL 24 jam, karena lirik gak berubah). */
    private val lyricsCache =
        object : LinkedHashMap<String, CachedLyrics>(48, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, CachedLyrics>?
            ) = size > MAX_LYRICS_CACHE
        }

    //  Search 

    suspend fun search(query: String): Result<List<RexTrack>> {
        val key = query.trim().lowercase()
        synchronized(searchCache) { searchCache[key] }
            ?.takeIf { it.fresh(SEARCH_TTL_MS) }
            ?.let { return Result.success(it.value) }

        return runApi {
            val res = withRetry { ApiClient.rexMusic.searchSpotify(query) }
            val list = res.result.orEmpty()
            when {
                list.isEmpty() -> emptyList()
                res.status != true -> error("pencarian gagal")
                else -> list.mapNotNull { it.toTrack(key) }
                    .distinctBy { it.id }
                    .also { synchronized(searchCache) { searchCache[key] = Cached(it) } }
            }
        }
    }

    //  Audio 

    suspend fun resolveAudio(track: RexTrack): Result<RexTrack> {
        if (track.spotifyUrl.isBlank()) return Result.failure(Exception("spotify url kosong"))

        synchronized(audioCache) { audioCache[track.spotifyUrl] }
            ?.takeIf { it.fresh(AUDIO_TTL_MS) }
            ?.let { return Result.success(track.copy(audioUrl = it.value)) }

        return runApi {
            val res = withRetry { ApiClient.rexMusic.downloadSpotify(track.spotifyUrl) }
            val r = res.result
            val url = r?.url
            if (res.status != true || r == null || url.isNullOrBlank()) error("gagal resolve audio")
            synchronized(audioCache) { audioCache[track.spotifyUrl] = Cached(url) }
            track.copy(
                audioUrl = url,
                title = track.title.takeUnless { it.isBlank() || it == "Unknown" } ?: r.title ?: track.title,
                artist = track.artist.takeUnless { it.isBlank() || it == "Unknown" } ?: r.artist ?: track.artist
            )
        }
    }

    suspend fun play(query: String): Result<RexTrack> {
        val list = search(query).getOrElse { return Result.failure(it) }
        val first = list.firstOrNull() ?: return Result.failure(Exception("lagu tidak ditemukan"))
        return resolveAudio(first)
    }

    //  Lyrics 

    suspend fun fetchLyrics(query: String): Result<Lyrics> {
        val cleanQuery = query.trim()
        val key = cleanQuery.lowercase()
        if (key.isBlank()) return Result.failure(Exception("query kosong"))

        synchronized(lyricsCache) { lyricsCache[key] }
            ?.takeIf { it.fresh() }
            ?.let { return Result.success(it.value) }

        return try {
            val lyrics = fetchNexrayLyrics(cleanQuery)
            if (lyrics.isEmpty) error("lirik tidak tersedia")
            synchronized(lyricsCache) { lyricsCache[key] = CachedLyrics(lyrics) }
            Result.success(lyrics)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(friendlyMessage(e), e))
        }
    }

    /**
     * Sumber synced lyrics RexMusic: GET https://api.nexray.eu.cc/search/lyrics?q=...
     * Timing diambil langsung dari field result.lyrics.synced_lyrics.
     */
    private suspend fun fetchNexrayLyrics(query: String): Lyrics = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        val url = URL("$LYRICS_ENDPOINT?q=$encoded")
        val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 15_000
            useCaches = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "RexMusic/1.0")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("server lyrics membalas kode $code")
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseNexrayLyrics(body)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseNexrayLyrics(body: String): Lyrics {
        val root = JSONObject(body)
        if (!root.optBoolean("status", false)) error("API lyrics gagal")
        val result = root.optJSONObject("result") ?: error("respons lyrics tidak valid")
        val data = result.optJSONObject("lyrics") ?: error("data lyrics tidak tersedia")
        val plain = data.optString("plain_lyrics", "")
        val syncedRaw = data.optString("synced_lyrics", "")
        val duration = data.optInt("duration", 0).coerceAtLeast(0)
        val synced = parseSyncedLyrics(syncedRaw)
        return Lyrics(
            plain = plain,
            synced = synced,
            durationSeconds = duration
        )
    }

    private val SYNCED_LINE_REGEX = Regex("""^\s*\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]\s*(.*?)\s*$""")

    private fun parseSyncedLyrics(raw: String): List<LyricLine> {
        if (raw.isBlank()) return emptyList()
        val out = ArrayList<LyricLine>(128)
        raw.lineSequence().forEach { line ->
            val match = SYNCED_LINE_REGEX.matchEntire(line) ?: return@forEach
            val minute = match.groupValues[1].toLongOrNull() ?: return@forEach
            val second = match.groupValues[2].toLongOrNull() ?: return@forEach
            val fractionRaw = match.groupValues.getOrNull(3).orEmpty()
            val fraction = fractionRaw.toLongOrNull() ?: 0L
            val fractionMs = when (fractionRaw.length) {
                3 -> fraction
                2 -> fraction * 10L
                1 -> fraction * 100L
                else -> 0L
            }
            val text = match.groupValues[4].trim()
            if (text.isBlank()) return@forEach
            val timeMs = minute * 60_000L + second * 1_000L + fractionMs
            out += LyricLine(
                timeMs = timeMs,
                text = text,
                sourceTimestamp = match.groupValues[0].trim().substringBefore("]").removePrefix("[")
            )
        }
        return out.sortedWith(compareBy<LyricLine> { it.timeMs }.thenBy { it.text })
    }
    //  Cache 

    /** Dipanggil kalau player error (kemungkinan link audio sudah expire). */
    fun invalidateAudio(track: RexTrack) {
        synchronized(audioCache) { audioCache.remove(track.spotifyUrl) }
    }

    fun clearCache() {
        synchronized(searchCache) { searchCache.clear() }
        synchronized(audioCache) { audioCache.clear() }
        synchronized(lyricsCache) { lyricsCache.clear() }
    }

    //  Helpers 

    private fun SpotifySearchItem.toTrack(fallbackId: String): RexTrack? {
        if (title.isNullOrBlank() && url.isNullOrBlank()) return null
        return RexTrack(
            id = url ?: title ?: fallbackId,
            title = title ?: "Unknown",
            artist = artist ?: "Unknown",
            album = album ?: "Single",
            cover = thumbnail.orEmpty(),
            spotifyUrl = url.orEmpty(),
            durationText = duration.orEmpty()
        )
    }

    /** Retry hanya untuk masalah jaringan (IOException), bukan error 4xx/parsing. */
    private suspend fun <T> withRetry(times: Int = 2, block: suspend () -> T): T {
        var last: IOException? = null
        repeat(times) { attempt ->
            try {
                return block()
            } catch (e: IOException) {
                last = e
                if (attempt < times - 1) delay(600L * (attempt + 1))
            }
        }
        throw last ?: IOException("gagal terhubung")
    }

    /** Bungkus ke Result tapi CancellationException tetap dilempar ulang. */
    private inline fun <T> runApi(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(Exception(friendlyMessage(e), e))
    }

    private fun friendlyMessage(e: Exception): String = when (e) {
        is UnknownHostException -> "tidak ada koneksi internet"
        is SocketTimeoutException -> "koneksi timeout, coba lagi"
        else -> e.message ?: "terjadi kesalahan"
    }
}
