package com.rexaps.rexmusic.widget

import com.rexaps.rexmusic.RexMusicUiState

internal fun RexMusicWidget.Companion.__splitImpl_lyricLines(state: RexMusicUiState): Lyr {
            if (state.nowPlaying == null) return Lyr("", "")
            val lyrics = state.lyrics
            return when {
                state.lyricsLoading -> Lyr("Memuat lirik…", "")
                lyrics.hasSynced -> {
                    val i = activeIndex(lyrics.synced, state.positionMs)
                    val cur = if (i >= 0) lyrics.synced[i].text.trim() else ""
                    val nxt = lyrics.synced.getOrNull(i + 1)?.text?.trim().orEmpty()
                    Lyr(if (cur.isBlank()) "♪" else "♪  $cur", nxt)
                }
                !lyrics.isEmpty -> Lyr("Lirik tanpa sinkronisasi · buka aplikasi", "")
                else -> Lyr("Lirik tidak tersedia", "")
            }
        }
