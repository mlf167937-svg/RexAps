package com.rexaps.rexmusic

import com.rexaps.rextools.utils.ApiClient

object RexMusicApi {

    suspend fun play(query: String): Result<RexTrack> {
        return try {
            val res = ApiClient.rexMusic.play(query)
            val r = res.result
            if (res.status == true && r != null && !r.mp3.isNullOrBlank()) {
                Result.success(
                    RexTrack(
                        id = query,
                        title = r.title ?: query,
                        artist = r.author ?: "Unknown",
                        album = "YouTube",
                        cover = r.thumbnail ?: "",
                        audioUrl = r.mp3,
                        durationSec = r.duration ?: 0
                    )
                )
            } else {
                Result.failure(Exception("respon gagal atau mp3 kosong"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
