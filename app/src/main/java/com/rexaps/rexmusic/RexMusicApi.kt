package com.rexaps.rexmusic

import com.rexaps.rextools.utils.ApiClient
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

object RexMusicApi {

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

    // ───────────────────────── Search ─────────────────────────

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

    // ───────────────────────── Audio ─────────────────────────

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

    // ───────────────────────── Lyrics ─────────────────────────

    suspend fun fetchLyrics(query: String): Result<Lyrics> {
        val key = query.trim().lowercase()
        if (key.isBlank()) return Result.failure(Exception("query kosong"))

        synchronized(lyricsCache) { lyricsCache[key] }
            ?.takeIf { it.fresh() }
            ?.let { return Result.success(it.value) }

        return runApi {
            val res = withRetry { ApiClient.rexMusic.searchLyrics(query) }
            val data = res.result?.lyrics
            val plain = data?.plainLyrics.orEmpty()
            val synced = parseSyncedLyrics(data?.syncedLyrics.orEmpty())
            val lyrics = Lyrics(plain = plain, synced = synced)
            if (lyrics.isEmpty) error("lirik tidak tersedia")
            synchronized(lyricsCache) { lyricsCache[key] = CachedLyrics(lyrics) }
            lyrics
        }
    }

    private val SYNCED_REGEX = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{2,3}))?]\s*(.*)""")

    private fun parseSyncedLyrics(raw: String): List<LyricLine> {
        if (raw.isBlank()) return emptyList()
        val out = ArrayList<LyricLine>(128)
        raw.lineSequence().forEach { line ->
            val m = SYNCED_REGEX.find(line) ?: return@forEach
            val min = m.groupValues[1].toLongOrNull() ?: return@forEach
            val sec = m.groupValues[2].toLongOrNull() ?: return@forEach
            val fracRaw = m.groupValues[3]
            val frac = fracRaw.toLongOrNull() ?: 0L
            val fracMs = when (fracRaw.length) {
                3 -> frac
                2 -> frac * 10
                else -> 0L
            }
            val text = m.groupValues[4].trim()
            if (text.isEmpty()) return@forEach
            out += LyricLine(min * 60_000 + sec * 1000 + fracMs, text)
        }
        return out
    }

    // ───────────────────────── Cache ─────────────────────────

    /** Dipanggil kalau player error (kemungkinan link audio sudah expire). */
    fun invalidateAudio(track: RexTrack) {
        synchronized(audioCache) { audioCache.remove(track.spotifyUrl) }
    }

    fun clearCache() {
        synchronized(searchCache) { searchCache.clear() }
        synchronized(audioCache) { audioCache.clear() }
        synchronized(lyricsCache) { lyricsCache.clear() }
    }

    // ───────────────────────── Helpers ─────────────────────────

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
