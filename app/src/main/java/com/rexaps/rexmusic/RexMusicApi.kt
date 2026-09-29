package com.rexaps.rexmusic

import com.rexaps.rextools.utils.ApiClient

object RexMusicApi {

    suspend fun search(query: String): Result<List<RexTrack>> {
        return try {
            val res = ApiClient.rexMusic.searchSpotify(query)
            val list = res.result
            if (res.status == true && !list.isNullOrEmpty()) {
                val tracks = list.map { item ->
                    RexTrack(
                        id = item.url ?: item.title ?: query,
                        title = item.title ?: "Unknown",
                        artist = item.artist ?: "Unknown",
                        album = item.album ?: "Single",
                        cover = item.thumbnail ?: "",
                        spotifyUrl = item.url ?: "",
                        durationText = item.duration ?: ""
                    )
                }
                Result.success(tracks)
            } else {
                Result.failure(Exception("hasil kosong"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveAudio(track: RexTrack): Result<RexTrack> {
        return try {
            if (track.spotifyUrl.isBlank()) {
                return Result.failure(Exception("spotify url kosong"))
            }
            val res = ApiClient.rexMusic.downloadSpotify(track.spotifyUrl)
            val r = res.result
            if (res.status == true && r != null && !r.url.isNullOrBlank()) {
                Result.success(
                    track.copy(
                        audioUrl = r.url,
                        title = r.title ?: track.title,
                        artist = r.artist ?: track.artist
                    )
                )
            } else {
                Result.failure(Exception("gagal resolve audio"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun play(query: String): Result<RexTrack> {
        val search = search(query).getOrElse { return Result.failure(it) }
        val first = search.firstOrNull() ?: return Result.failure(Exception("lagu tidak ditemukan"))
        return resolveAudio(first)
    }
}
