package com.rexaps.rexmusic

/** Nama folder lagu: "Iqro" + "Raim Laode" -> "Iqro-Raim-Laode". */
fun offlineKey(track: RexTrack): String {
    val raw = KEY_CLEAN.replace("${track.title} ${track.artist}", "-").trim('-')
    return raw.take(100).trim('-').ifBlank { "Lagu" }
}
