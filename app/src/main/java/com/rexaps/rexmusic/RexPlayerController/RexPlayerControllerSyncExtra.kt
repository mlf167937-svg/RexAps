package com.rexaps.rexmusic

/** Local playback gain for RexMusicSync; does not change Android system volume. */
    internal fun RexPlayerController.__splitExtra_setSyncVolume_35(volume: Float) {
        syncVolume = volume.coerceIn(0f, 1f)
        runCatching { player?.setVolume(syncVolume, syncVolume) }
    }
