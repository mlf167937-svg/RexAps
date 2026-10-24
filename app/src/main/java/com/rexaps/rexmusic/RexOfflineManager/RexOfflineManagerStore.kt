package com.rexaps.rexmusic

import java.io.File
import org.json.JSONObject

internal fun RexOfflineManager.__split_writeMeta_12(dir: File, t: RexTrack) {
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

internal fun RexOfflineManager.__split_writeLyrics_13(dir: File, lyrics: Lyrics?, unavailable: Boolean) {
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
