package com.rexaps.rexmusic

import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal val KEY_CLEAN = Regex("[^\\p{L}\\p{N}]+")

/** Key folder lagu ini kalau sudah diunduh (cocok lewat nama folder, atau lewat id). */
fun OfflineState.keyFor(track: RexTrack, key: String = offlineKey(track)): String? =
    if (key in keys) key else entries.firstOrNull { it.track.id == track.id }?.key

@Synchronized internal
    fun RexOfflineManager.__split_init_1(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        RexDownloadNotification.init(appContext)
        _state.update {
            it.copy(
                enabled = prefs.getBoolean(KEY_OFFLINE, false),
                hasAccess = hasAccess(appContext)
            )
        }
        refresh()
    }

fun RexOfflineManager.__split_delete_7(track: RexTrack) {
        val key = _state.value.keyFor(track) ?: return
        jobs[key]?.cancel()
        scope.launch {
            withContext(Dispatchers.IO) { File(root, key).deleteRecursively() }
            refreshNow()
            _messages.tryEmit("Dihapus dari offline: ${track.title}")
        }
    }

fun RexOfflineManager.__split_deleteAll_8() {
        val keys = _state.value.entries.map { it.key }
        scope.launch {
            withContext(Dispatchers.IO) { keys.forEach { File(root, it).deleteRecursively() } }
            refreshNow()
            _messages.tryEmit("Semua lagu offline dihapus")
        }
    }

internal fun RexOfflineManager.__split_cleanup_15(key: String) {
        val dir = File(root, key)
        File(dir, "$AUDIO_NAME.part").delete()
        File(dir, "thumbnail.tmp").delete()
        if (!File(dir, AUDIO_NAME).exists()) dir.deleteRecursively()
    }

internal fun RexOfflineManager.__split_scan_16(): List<OfflineEntry> {
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
