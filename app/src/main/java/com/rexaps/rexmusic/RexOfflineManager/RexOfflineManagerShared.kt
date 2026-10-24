package com.rexaps.rexmusic

private val KEY_CLEAN = Regex("[^\\p{L}\\p{N}]+")

/** Nama folder lagu: "Iqro" + "Raim Laode" -> "Iqro-Raim-Laode". */
fun offlineKey(track: RexTrack): String {
    val raw = KEY_CLEAN.replace("${track.title} ${track.artist}", "-").trim('-')
    return raw.take(100).trim('-').ifBlank { "Lagu" }
}

/** Key folder lagu ini kalau sudah diunduh (cocok lewat nama folder, atau lewat id). */
fun OfflineState.keyFor(track: RexTrack, key: String = offlineKey(track)): String? =
    if (key in keys) key else entries.firstOrNull { it.track.id == track.id }?.key

fun OfflineState.isDownloaded(track: RexTrack, key: String = offlineKey(track)): Boolean =
    keyFor(track, key) != null

internal const val DISPLAY_PATH = "Download/RexAps/Music"

internal const val AUDIO_NAME = "music.mp3"

internal const val META_NAME = "meta.json"

internal const val LYRICS_NAME = "lyrics.json"

internal const val PREFS = "rex_music_offline"

internal const val KEY_OFFLINE = "offline_mode"

internal const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13) RexMusic"

internal const val MAX_PARALLEL = 2
