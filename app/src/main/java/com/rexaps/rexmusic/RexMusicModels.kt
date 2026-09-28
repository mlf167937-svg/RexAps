package com.rexaps.rexmusic

data class RexTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "Songs",
    val cover: String = "",
    val audioUrl: String = "",
    val durationSec: Int = 0
)

data class RexMusicUiState(
    val loading: Boolean = false,
    val tracks: List<RexTrack> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val error: String? = null
)

data class YtPlayResponse(
    val status: Boolean? = null,
    val creator: String? = null,
    val source: String? = null,
    val result: YtPlayResult? = null
)

data class YtPlayResult(
    val title: String? = null,
    val url: String? = null,
    val mp3: String? = null,
    val thumbnail: String? = null,
    val duration: Int? = null,
    val duration_timestamp: String? = null,
    val views: Long? = null,
    val published: String? = null,
    val author: String? = null
)
