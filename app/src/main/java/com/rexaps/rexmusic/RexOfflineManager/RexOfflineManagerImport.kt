package com.rexaps.rexmusic

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

/** Import a user-selected MP3 into the existing offline library, preserving source metadata. */
internal fun RexOfflineManager.__splitImpl_importLocalAudio(
    uri: Uri,
    source: RexTrackSource,
    variant: String,
    titleOverride: String,
    artistOverride: String,
    thumbnailUri: Uri?,
    thumbnailUrl: String
) {
    if (!appContextInitialized || !hasAccess(appContext)) {
        _messages.tryEmit("Izin penyimpanan belum diberikan")
        return
    }
    scope.launch {
        try {
            val imported = withContext(Dispatchers.IO) {
                val resolver = appContext.contentResolver
                val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                    .orEmpty()
                    .ifBlank { "Lagu lokal.mp3" }
                val mime = resolver.getType(uri).orEmpty().lowercase()
                require(displayName.endsWith(".mp3", true) || mime == "audio/mpeg" || mime == "audio/mp3") {
                    "Pilih file MP3. Format selain MP3 belum didukung untuk impor ini."
                }

                val retriever = MediaMetadataRetriever()
                val metadata = runCatching {
                    retriever.setDataSource(appContext, uri)
                    val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    val artwork = retriever.embeddedPicture
                    LocalAudioMetadata(title, artist, album, duration, artwork)
                }.getOrElse { LocalAudioMetadata() }
                runCatching { retriever.release() }

                val fallbackTitle = displayName.substringBeforeLast('.', displayName).trim().ifBlank { "Lagu lokal" }
                val title = titleOverride.trim().takeIf { it.isNotBlank() }
                    ?: metadata.title?.takeIf { it.isNotBlank() }
                    ?: fallbackTitle
                val artist = artistOverride.trim().takeIf { it.isNotBlank() }
                    ?: metadata.artist?.takeIf { it.isNotBlank() }
                    ?: "File lokal"
                var uniqueTitle = title
                var track = RexTrack(
                    id = "local-${System.currentTimeMillis()}-${title.hashCode()}",
                    title = uniqueTitle,
                    artist = artist,
                    album = metadata.album?.takeIf { it.isNotBlank() } ?: "Offline",
                    cover = thumbnailUrl.trim(),
                    durationText = metadata.durationMs.takeIf { it > 0L }?.let(::formatDuration) ?: "",
                    source = source,
                    sourceUrl = "",
                    variant = variant
                )
                var suffix = 2
                while (File(root, offlineKey(track)).exists()) {
                    uniqueTitle = "$title ($suffix)"
                    track = track.copy(id = "local-${System.currentTimeMillis()}-${suffix}", title = uniqueTitle)
                    suffix++
                }
                val directory = File(root, offlineKey(track))
                if (!directory.exists() && !directory.mkdirs()) error("Tidak dapat membuat folder penyimpanan")
                val part = File(directory, "$AUDIO_NAME.part")
                try {
                    resolver.openInputStream(uri)?.use { input ->
                        part.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("File tidak dapat dibaca")
                    if (part.length() <= 0L) error("File MP3 kosong")
                    val target = File(directory, AUDIO_NAME)
                    if (target.exists()) target.delete()
                    if (!part.renameTo(target)) error("Gagal menyimpan file MP3")
                    val customThumbnailSaved = when {
                        thumbnailUri != null -> runCatching {
                            saveUserThumbnailFromUri(appContext.contentResolver, thumbnailUri, directory)
                        }.getOrDefault(false)
                        thumbnailUrl.isNotBlank() -> runCatching {
                            saveUserThumbnailFromUrl(thumbnailUrl, directory)
                        }.getOrDefault(false)
                        else -> false
                    }
                    if (!customThumbnailSaved) {
                        metadata.artwork?.takeIf { it.isNotEmpty() }?.let { bytes ->
                            runCatching { File(directory, "thumbnail.jpg").writeBytes(bytes) }
                        }
                    }
                    writeMeta(directory, track)
                } catch (error: Exception) {
                    part.delete()
                    if (!File(directory, AUDIO_NAME).exists()) directory.deleteRecursively()
                    throw error
                }
                track
            }
            refreshNow()
            _messages.tryEmit("MP3 ditambahkan: ${imported.title}")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            _messages.tryEmit("Gagal mengimpor MP3: ${error.message ?: "kesalahan file"}")
        }
    }
}

private const val MAX_USER_THUMBNAIL_BYTES = 8L * 1024L * 1024L

private fun saveUserThumbnailFromUri(
    resolver: android.content.ContentResolver,
    uri: Uri,
    directory: File
): Boolean {
    val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else "" }
        .orEmpty()
    val mime = resolver.getType(uri).orEmpty().lowercase()
    val extension = when {
        mime == "image/png" || displayName.endsWith(".png", true) -> "png"
        mime == "image/webp" || displayName.endsWith(".webp", true) -> "webp"
        mime == "image/jpeg" || displayName.endsWith(".jpg", true) || displayName.endsWith(".jpeg", true) -> "jpg"
        else -> throw IllegalArgumentException("Thumbnail harus berupa JPG, PNG, atau WEBP")
    }
    require(mime.startsWith("image/") || displayName.matches(Regex("(?i).*\\.(jpg|jpeg|png|webp)$"))) {
        "File thumbnail bukan gambar"
    }
    val target = File(directory, "thumbnail.$extension")
    val temp = File(directory, "thumbnail.image.part")
    try {
        resolver.openInputStream(uri)?.use { input ->
            temp.outputStream().use { output -> copyBounded(input, output, MAX_USER_THUMBNAIL_BYTES) }
        } ?: throw IllegalStateException("Foto thumbnail tidak dapat dibaca")
        if (!temp.isFile || temp.length() <= 0L) throw IllegalStateException("Foto thumbnail kosong")
        if (target.exists()) target.delete()
        if (!temp.renameTo(target)) throw IllegalStateException("Gagal menyimpan foto thumbnail")
        return true
    } catch (error: Exception) {
        temp.delete()
        throw error
    }
}

private fun saveUserThumbnailFromUrl(rawUrl: String, directory: File): Boolean {
    val parsed = java.net.URI(rawUrl.trim())
    require(parsed.scheme in setOf("http", "https") && !parsed.host.isNullOrBlank()) {
        "Link thumbnail tidak valid"
    }
    val path = parsed.path.orEmpty().lowercase()
    require(path.endsWith(".jpg") || path.endsWith(".jpeg")) {
        "Link thumbnail harus mengarah ke file JPG"
    }
    val connection = (java.net.URL(rawUrl.trim()).openConnection() as java.net.HttpURLConnection).apply {
        connectTimeout = 12_000
        readTimeout = 18_000
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) RexMusic")
        setRequestProperty("Accept", "image/jpeg,image/*;q=0.8")
    }
    val temp = File(directory, "thumbnail.image.part")
    try {
        val code = connection.responseCode
        require(code in 200..299) { "Server thumbnail membalas kode $code" }
        val contentType = connection.contentType.orEmpty().substringBefore(';').lowercase()
        require(contentType.isBlank() || contentType.startsWith("image/")) {
            "URL tersebut bukan file gambar"
        }
        connection.inputStream.use { input ->
            temp.outputStream().use { output -> copyBounded(input, output, MAX_USER_THUMBNAIL_BYTES) }
        }
        if (!temp.isFile || temp.length() <= 0L) throw IllegalStateException("Thumbnail JPG kosong")
        val target = File(directory, "thumbnail.jpg")
        if (target.exists()) target.delete()
        if (!temp.renameTo(target)) throw IllegalStateException("Gagal menyimpan thumbnail JPG")
        return true
    } catch (error: Exception) {
        temp.delete()
        throw error
    } finally {
        connection.disconnect()
    }
}

private fun copyBounded(
    input: java.io.InputStream,
    output: java.io.OutputStream,
    maxBytes: Long
): Long {
    val buffer = ByteArray(8 * 1024)
    var total = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read
        require(total <= maxBytes) { "Ukuran thumbnail melebihi 8 MB" }
        output.write(buffer, 0, read)
    }
    return total
}

private data class LocalAudioMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long = 0L,
    val artwork: ByteArray? = null
)

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}

