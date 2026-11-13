package com.rexaps.rexmusic

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URLEncoder
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal data class RexCatalogSearchOutcome(
    val tracks: List<RexTrack>,
    val requestedProviders: List<RexTrackSource>,
    val failedProviders: List<RexTrackSource>
)

/** Search adapter for catalog metadata. Provider page URLs are never treated as playable audio URLs. */
object RexMusicCatalogApi {
    private const val BASE_URL = "https://api.nexray.eu.cc/search"
    private const val MAX_RESPONSE_BYTES = 4 * 1024 * 1024
    private val catalogProviders = listOf(
        RexTrackSource.SPOTIFY,
        RexTrackSource.APPLE_MUSIC,
        RexTrackSource.YOUTUBE
    )

    /** Compatibility facade for existing call sites. */
    suspend fun searchHybrid(query: String): Result<List<RexTrack>> = runCatching {
        searchHybridDetailed(query).let { outcome ->
            if (outcome.tracks.isEmpty() && outcome.failedProviders.size == outcome.requestedProviders.size) {
                throw IOException("Semua provider gagal: ${outcome.failedProviders.joinToString { it.label }}")
            }
            outcome.tracks
        }
    }.onFailure { if (it is CancellationException) throw it }

    /** One-provider requests stay isolated; All runs available providers concurrently. */
    internal suspend fun searchHybridDetailed(
        query: String,
        provider: RexTrackSource? = null
    ): RexCatalogSearchOutcome {
        val cleanQuery = normalizeQuery(query)
        require(cleanQuery.isNotBlank()) { "Masukkan judul lagu atau nama artis" }
        val requested = (if (provider == null) catalogProviders else listOf(provider))
            .filter { it != RexTrackSource.LOCAL }
        if (requested.isEmpty()) return RexCatalogSearchOutcome(emptyList(), emptyList(), emptyList())

        val results = supervisorScope {
            requested.map { source ->
                async {
                    try {
                        source to search(source, cleanQuery).getOrThrow()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        source to null
                    }
                }
            }.map { it.await() }
        }
        val failed = results.filter { it.second == null }.map { it.first }
        val merged = results.flatMap { it.second.orEmpty() }
            .distinctBy { "${it.source.id}:${it.id}" }
        return RexCatalogSearchOutcome(merged, requested, failed)
    }

    suspend fun search(
        source: RexTrackSource,
        query: String,
        variant: String = ""
    ): Result<List<RexTrack>> {
        val cleanQuery = normalizeQuery(query)
        if (cleanQuery.isBlank()) return Result.failure(IllegalArgumentException("Masukkan judul lagu atau nama artis"))
        if (source == RexTrackSource.LOCAL) return Result.success(emptyList())

        return try {
            val searchQuery = buildString {
                append(cleanQuery)
                if (source == RexTrackSource.YOUTUBE && variant.isNotBlank() &&
                    !variant.equals("Original", ignoreCase = true) &&
                    !cleanQuery.contains(variant, ignoreCase = true)
                ) {
                    append(' ')
                    append(variant.trim())
                }
            }
            val tracks = when (source) {
                RexTrackSource.SPOTIFY -> RexMusicApi.search(searchQuery).getOrThrow().map {
                    it.copy(
                        source = RexTrackSource.SPOTIFY,
                        sourceUrl = it.spotifyUrl.ifBlank { it.sourceUrl }
                    )
                }
                RexTrackSource.YOUTUBE -> searchYouTube(searchQuery, variant)
                RexTrackSource.APPLE_MUSIC -> searchAppleMusic(searchQuery, variant)
                RexTrackSource.LOCAL -> emptyList()
            }
            Result.success(tracks)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(Exception(friendlyMessage(error), error))
        }
    }

    private suspend fun searchYouTube(query: String, variant: String): List<RexTrack> {
        val root = request("youtube", query)
        val items = root.optJSONArray("result") ?: return emptyList()
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                if (!item.optString("type").equals("video", ignoreCase = true)) continue
                val title = item.optCleanString("title")
                if (title.isBlank()) continue
                val rawId = item.optCleanString("id")
                val validId = rawId.takeIf { VIDEO_ID.matches(it) }
                val rawUrl = item.optCleanString("url")
                val url = when {
                    rawUrl.isNotBlank() && isValidProviderLink(RexTrackSource.YOUTUBE, rawUrl) -> rawUrl
                    validId != null -> "https://www.youtube.com/watch?v=$validId"
                    else -> continue
                }
                val id = validId ?: URI(url).rawQuery?.split('&')
                    ?.firstOrNull { it.startsWith("v=") }?.substringAfter('=')?.takeIf(VIDEO_ID::matches)
                    ?: url
                add(
                    RexTrack(
                        id = "youtube:$id",
                        title = title,
                        artist = item.optCleanString("channel").ifBlank { "YouTube" },
                        album = "YouTube",
                        cover = validImageUrl(item.optCleanString("image_url")),
                        durationText = item.optCleanString("duration"),
                        source = RexTrackSource.YOUTUBE,
                        sourceUrl = url,
                        variant = variant.takeUnless { it.equals("Original", true) }.orEmpty(),
                        channel = item.optCleanString("channel").ifBlank { null },
                        durationSeconds = item.optLongOrNull("seconds"),
                        views = item.optCleanString("views").ifBlank { null },
                        uploadAt = item.optCleanString("upload_at").ifBlank { null }
                    )
                )
            }
        }.distinctBy { it.id }
    }

    private suspend fun searchAppleMusic(query: String, variant: String): List<RexTrack> {
        val root = request("applemusic", query)
        val items = root.optJSONArray("result") ?: return emptyList()
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val artist = appleSongArtist(item.optString("subtitle")) ?: continue
                val title = item.optCleanString("title")
                val link = item.optCleanString("link")
                if (title.isBlank() || !isValidProviderLink(RexTrackSource.APPLE_MUSIC, link)) continue
                val id = runCatching { URI(link).path?.trimEnd('/')?.substringAfterLast('/') }.getOrNull()
                    ?.takeIf { it.isNotBlank() } ?: link
                add(
                    RexTrack(
                        id = "apple:$id",
                        title = title,
                        artist = artist,
                        album = "Apple Music",
                        cover = validImageUrl(item.optCleanString("image")),
                        source = RexTrackSource.APPLE_MUSIC,
                        sourceUrl = link,
                        variant = variant.takeUnless { it.equals("Original", true) }.orEmpty()
                    )
                )
            }
        }.distinctBy { it.sourceUrl }
    }

    private fun appleSongArtist(rawSubtitle: String): String? {
        val subtitle = rawSubtitle.trim()
        if (subtitle.isBlank()) return null
        if (subtitle.equals("Song", ignoreCase = true)) return "Apple Music"
        val separators = listOf(" · ", " • ", " — ", " – ", " - ", " | ")
        for (separator in separators) {
            val split = subtitle.indexOf(separator)
            if (split <= 0) continue
            val kind = subtitle.substring(0, split).trim()
            if (kind.equals("Song", ignoreCase = true)) {
                return subtitle.substring(split + separator.length).trim().ifBlank { "Apple Music" }
            }
        }
        val byMarker = subtitle.indexOf("Song by ", ignoreCase = true)
        if (byMarker == 0) return subtitle.substring(8).trim().ifBlank { "Apple Music" }
        // Without a clearly identified Song type, never guess that an album/artist is a song.
        return null
    }

    private suspend fun request(provider: String, query: String): JSONObject = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(normalizeQuery(query), Charsets.UTF_8.name())
        val connection = (URI("$BASE_URL/$provider?q=$encoded").toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 18_000
            useCaches = true
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "RexMusic/2.1")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("API $provider membalas kode $code")
            val contentType = connection.contentType.orEmpty().lowercase()
            if (contentType.isNotBlank() && !contentType.contains("json")) {
                throw IOException("Respons $provider bukan JSON")
            }
            val body = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_RESPONSE_BYTES) throw IOException("Respons $provider terlalu besar")
                    output.write(buffer, 0, count)
                }
                output.toByteArray().toString(Charsets.UTF_8)
            }
            val root = JSONObject(body)
            if (!root.optBoolean("status", false)) throw IOException("Pencarian $provider gagal")
            root
        } finally {
            connection.disconnect()
        }
    }

    fun isValidProviderLink(source: RexTrackSource, rawUrl: String): Boolean {
        val uri = runCatching { URI(rawUrl.trim()) }.getOrNull() ?: return false
        if (uri.scheme?.lowercase() != "https" || uri.userInfo != null || uri.host.isNullOrBlank()) return false
        val host = uri.host.lowercase()
        return when (source) {
            RexTrackSource.SPOTIFY -> host == "spotify.com" || host.endsWith(".spotify.com") || host == "spotify.link"
            RexTrackSource.YOUTUBE -> host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com")
            RexTrackSource.APPLE_MUSIC -> host == "music.apple.com" || host == "itunes.apple.com"
            RexTrackSource.LOCAL -> false
        }
    }

    private fun validImageUrl(raw: String): String {
        val value = raw.trim()
        val uri = runCatching { URI(value) }.getOrNull() ?: return ""
        return value.takeIf { uri.scheme?.lowercase() in setOf("https", "http") && uri.host != null && uri.userInfo == null }.orEmpty()
    }

    private fun normalizeQuery(query: String): String = query.trim().replace(Regex("\\s+"), " ")

    private fun friendlyMessage(error: Exception): String = when (error) {
        is UnknownHostException -> "Tidak ada koneksi internet"
        is SocketTimeoutException -> "Koneksi timeout, coba lagi"
        else -> error.message ?: "Pencarian gagal"
    }

    private fun JSONObject.optCleanString(name: String): String = optString(name).trim()
        .takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }.orEmpty()

    private fun JSONObject.optLongOrNull(name: String): Long? = when (val value = opt(name)) {
        is Number -> value.toLong().takeIf { it >= 0L }
        is String -> value.trim().toLongOrNull()?.takeIf { it >= 0L }
        else -> null
    }

    private val VIDEO_ID = Regex("[A-Za-z0-9_-]{11}")
}
