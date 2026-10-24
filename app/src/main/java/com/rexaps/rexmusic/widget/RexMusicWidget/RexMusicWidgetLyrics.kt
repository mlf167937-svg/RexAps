package com.rexaps.rexmusic.widget

import com.rexaps.rexmusic.RexMusicUiState

internal fun RexMusicWidget.Companion.__splitImpl_lyricLines(state: RexMusicUiState): RexMusicWidget.Lyr {
            if (state.nowPlaying == null) return RexMusicWidget.Lyr("", "")
            val lyrics = state.lyrics
            return when {
                state.lyricsLoading -> RexMusicWidget.Lyr("Memuat lirik…", "")
                lyrics.hasSynced -> {
                    val i = activeIndex(lyrics.synced, state.positionMs)
                    val cur = if (i >= 0) lyrics.synced[i].text.trim() else ""
                    val nxt = lyrics.synced.getOrNull(i + 1)?.text?.trim().orEmpty()
                    RexMusicWidget.Lyr(if (cur.isBlank()) "♪" else "♪  $cur", nxt)
                }
                !lyrics.isEmpty -> RexMusicWidget.Lyr("Lirik tanpa sinkronisasi · buka aplikasi", "")
                else -> RexMusicWidget.Lyr("Lirik tidak tersedia", "")
            }
        }
