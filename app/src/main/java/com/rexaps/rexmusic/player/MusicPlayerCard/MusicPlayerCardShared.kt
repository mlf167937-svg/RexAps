package com.rexaps.rexmusic.player

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.rexaps.rexmusic.*

@Stable
class PlayerActions(
    val onClose: () -> Unit,
    val onLyrics: () -> Unit,
    val onMore: () -> Unit,
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrev: () -> Unit,
    val onOpenPrevious: () -> Unit,
    val onSeek: (Float) -> Unit,
    val onToggleLike: () -> Unit,
    val onRepeat: () -> Unit,
    val onAutoplay: () -> Unit,
    val onQueue: () -> Unit,
    val onDownload: () -> Unit,
    val onCancelDownload: () -> Unit,
    val onDelete: () -> Unit
)

internal fun statusLabel(state: RexMusicUiState): String = when (state.phase) {
    LoadPhase.Resolving -> "MENYIAPKAN"
    LoadPhase.Preparing ->
        if (state.bufferedPercent in 1..99) "MEMUAT ${state.bufferedPercent}%" else "MEMUAT"
    LoadPhase.Buffering -> "BUFFERING"
    else -> if (state.nowPlayingOffline) "DIPUTAR OFFLINE" else "SEDANG DIPUTAR"
}

internal fun activeLyricText(lyrics: Lyrics, positionMs: Long): String? {
    val list = lyrics.synced
    if (list.isEmpty()) return null
    var low = 0
    var high = list.lastIndex
    var active = -1
    while (low <= high) {
        val mid = (low + high) ushr 1
        if (list[mid].timeMs <= positionMs) {
            active = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return active.takeIf { it >= 0 }?.let { list[it].text.trim() }?.takeIf { it.isNotBlank() }
}

internal fun fmt(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
