package com.rexaps.rexmusic

data class RexTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "Single",
    val cover: String = "",
    val audioUrl: String = "",
    val spotifyUrl: String = "",
    val durationText: String = ""
)

/** Fase loading real-time. */
enum class LoadPhase { Idle, Searching, Resolving, Preparing, Buffering }

/** True kalau player sedang sibuk (mencari audio, menyiapkan, atau buffering/lag). */
val LoadPhase.isPlayerBusy: Boolean
    get() = this == LoadPhase.Resolving ||
        this == LoadPhase.Preparing ||
        this == LoadPhase.Buffering

data class RexMusicUiState(
    val loading: Boolean = false,
    val loadingText: String = "",
    val phase: LoadPhase = LoadPhase.Idle,
    val bufferedPercent: Int = 0,
    val tracks: List<RexTrack> = emptyList(),
    val searchResults: List<RexTrack> = emptyList(),
    val currentIndex: Int = 0,
    val nowPlaying: RexTrack? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val error: String? = null
)

data class SpotifySearchResponse(
    val status: Boolean? = null,
    val result: List<SpotifySearchItem>? = null
)

data class SpotifySearchItem(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val url: String? = null,
    val thumbnail: String? = null,
    val duration: String? = null
)

data class SpotifyDownloadResponse(
    val status: Boolean? = null,
    val result: SpotifyDownloadResult? = null
)

data class SpotifyDownloadResult(
    val url: String? = null,
    val title: String? = null,
    val artist: String? = null
)
