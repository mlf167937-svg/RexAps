package com.rexaps.rexmusic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

//  Access & mode 

/** Android 11+: "Akses semua file". Android 10-: izin tulis penyimpanan biasa. */
internal fun RexOfflineManager.hasAccessImpl(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

internal suspend fun RexOfflineManager.refreshNow() {
    val access = hasAccess(appContext)
    val entries = if (access) withContext(Dispatchers.IO) { scan() } else emptyList()
    _state.update {
        it.copy(
            hasAccess = access,
            entries = entries,
            keys = entries.map { e -> e.key }.toSet(),
            totalBytes = entries.sumOf { e -> e.sizeBytes }
        )
    }
}

/** Import lagu yang diterima lewat RexMusicSync ke library RexMusic, tanpa menghapus cache sync. */
internal fun RexOfflineManager.importSyncTrackImpl(track: RexTrack, sourceDir: File): Boolean {
    if (!::appContext.isInitialized || !hasAccess(appContext)) return false
    return runCatching {
        val audio = File(sourceDir, "music.mp3")
        require(audio.isFile && audio.length() > 0L) { "File audio tidak lengkap" }
        val key = offlineKey(track)
        val dest = File(root, key)
        dest.mkdirs()
        audio.copyTo(File(dest, AUDIO_NAME), overwrite = true)
        listOf("meta.json", "lyrics.json", "thumbnail.jpg", "thumbnail.png", "thumbnail.jpeg", "thumbnail.webp").forEach { name ->
            val src = File(sourceDir, name)
            if (src.isFile && src.length() > 0L) src.copyTo(File(dest, name), overwrite = true)
        }
        writeMeta(dest, track)
        refresh()
        true
    }.getOrDefault(false)
}

/** File mp3 lokal untuk lagu ini, atau null kalau belum diunduh. */
internal fun RexOfflineManager.audioFileImpl(track: RexTrack): File? {
    val key = _state.value.keyFor(track) ?: return null
    val file = File(root, "$key/$AUDIO_NAME")
    return file.takeIf { it.isFile && it.length() > 0L }
}

/** Lirik offline yang sudah disimpan bersama lagu. */
internal fun RexOfflineManager.lyricsFileImpl(track: RexTrack): File? {
    val key = _state.value.keyFor(track) ?: return null
    val file = File(root, "$key/$LYRICS_NAME")
    return file.takeIf { it.isFile && it.length() > 0L }
}

internal fun RexOfflineManager.readOfflineLyricsImpl(track: RexTrack): Lyrics? = runCatching {
    val file = lyricsFile(track) ?: return null
    val o = JSONObject(file.readText())
    val plain = o.optString("plainLyrics")
    val arr = o.optJSONArray("syncedLyrics") ?: org.json.JSONArray()
    val synced = (0 until arr.length()).mapNotNull { i ->
        val line = arr.optJSONObject(i) ?: return@mapNotNull null
        val text = line.optString("text").trim()
        if (text.isBlank()) return@mapNotNull null
        LyricLine(
            timeMs = line.optLong("timeMs").coerceAtLeast(0L),
            text = text,
            sourceTimestamp = line.optString("timestamp").ifBlank { null }
        )
    }.sortedBy { it.timeMs }
    Lyrics(
        plain = plain,
        synced = synced,
        durationSeconds = o.optInt("duration", 0).coerceAtLeast(0)
    )
}.getOrNull()

internal fun RexOfflineManager.deleteImpl(track: RexTrack) {
    val key = _state.value.keyFor(track) ?: return
    jobs[key]?.cancel()
    scope.launch {
        withContext(Dispatchers.IO) { File(root, key).deleteRecursively() }
        refreshNow()
        _messages.tryEmit("Dihapus dari offline: ${track.title}")
    }
}

internal fun RexOfflineManager.deleteAllImpl() {
    val keys = _state.value.entries.map { it.key }
    scope.launch {
        withContext(Dispatchers.IO) { keys.forEach { File(root, it).deleteRecursively() } }
        refreshNow()
        _messages.tryEmit("Semua lagu offline dihapus")
    }
}

internal fun RexOfflineManager.writeMeta(dir: File, t: RexTrack) {
    runCatching {
        File(dir, META_NAME).writeText(
            JSONObject()
                .put("id", t.id)
                .put("title", t.title)
                .put("artist", t.artist)
                .put("album", t.album)
                .put("spotifyUrl", t.spotifyUrl)
                .put("durationText", t.durationText)
                .put("coverUrl", t.cover)
                .toString()
        )
    }
}

internal fun RexOfflineManager.writeLyrics(dir: File, lyrics: Lyrics?, unavailable: Boolean) {
    runCatching {
        val synced = org.json.JSONArray().apply {
            lyrics?.synced.orEmpty().forEach { line ->
                put(
                    org.json.JSONObject()
                        .put("timeMs", line.timeMs)
                        .put("timestamp", line.sourceTimestamp.orEmpty())
                        .put("text", line.text)
                )
            }
        }
        File(dir, LYRICS_NAME).writeText(
            JSONObject()
                .put("version", 2)
                .put("available", lyrics != null && !lyrics.isEmpty)
                .put("offlineOnly", true)
                .put("unavailable", unavailable)
                .put("duration", lyrics?.durationSeconds ?: 0)
                .put("plainLyrics", lyrics?.plain.orEmpty())
                .put("syncedLyrics", synced)
                .toString(2)
        )
    }
}

private fun RexOfflineManager.readMeta(dir: File): JSONObject? = runCatching {
    JSONObject(File(dir, META_NAME).readText())
}.getOrNull()

internal fun RexOfflineManager.cleanup(key: String) {
    val dir = File(root, key)
    File(dir, "$AUDIO_NAME.part").delete()
    File(dir, "thumbnail.tmp").delete()
    if (!File(dir, AUDIO_NAME).exists()) dir.deleteRecursively()
}

//  Scan 

private fun RexOfflineManager.scan(): List<OfflineEntry> {
    val dirs = root.listFiles { f -> f.isDirectory } ?: return emptyList()
    return dirs.mapNotNull { dir ->
        val mp3 = File(dir, AUDIO_NAME)
        if (!mp3.isFile || mp3.length() <= 0L) return@mapNotNull null
        val thumb = findThumbnail(dir)
        val meta = readMeta(dir)
        val track = RexTrack(
            id = meta?.optString("id").orEmpty().ifBlank { "off-${dir.name}" },
            title = meta?.optString("title").orEmpty().ifBlank { dir.name.replace('-', ' ') },
            artist = meta?.optString("artist").orEmpty().ifBlank { "Unknown" },
            album = meta?.optString("album").orEmpty().ifBlank { "Offline" },
            cover = thumb?.let { Uri.fromFile(it).toString() }.orEmpty(),
            spotifyUrl = meta?.optString("spotifyUrl").orEmpty(),
            durationText = meta?.optString("durationText").orEmpty()
        )
        OfflineEntry(
            key = dir.name,
            track = track,
            sizeBytes = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() },
            savedAt = mp3.lastModified()
        )
    }.sortedByDescending { it.savedAt }
}

private fun RexOfflineManager.findThumbnail(dir: File): File? =
    listOf("thumbnail.jpg", "thumbnail.png", "thumbnail.jpeg", "thumbnail.webp")
        .map { File(dir, it) }
        .firstOrNull { it.isFile && it.length() > 0L }

//  Status helpers 

internal fun RexOfflineManager.setStatus(key: String, status: DownloadStatus) {
    _state.update { it.copy(downloads = it.downloads + (key to status)) }
    if (::appContext.isInitialized) RexDownloadNotification.showProgress(appContext, key, status)
}

internal fun RexOfflineManager.clearStatus(key: String) =
    _state.update { it.copy(downloads = it.downloads - key) }
