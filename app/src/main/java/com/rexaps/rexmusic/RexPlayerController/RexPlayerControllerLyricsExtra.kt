package com.rexaps.rexmusic

import kotlinx.coroutines.flow.update

internal fun RexPlayerController.__splitExtra_toggleLyrics_27() = _state.update { it.copy(lyricsVisible = !it.lyricsVisible) }

internal fun RexPlayerController.__splitExtra_closeLyrics_28() = _state.update { it.copy(lyricsVisible = false) }
