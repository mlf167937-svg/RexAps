package com.rexaps.rexmusic.player

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.rexaps.rexmusic.*

internal fun statusLabel(state: RexMusicUiState): String = when (state.phase) {
    LoadPhase.Resolving -> "MENYIAPKAN"
    LoadPhase.Preparing ->
        if (state.bufferedPercent in 1..99) "MEMUAT ${state.bufferedPercent}%" else "MEMUAT"
    LoadPhase.Buffering -> "BUFFERING"
    else -> if (state.nowPlayingOffline) "DIPUTAR OFFLINE" else "SEDANG DIPUTAR"
}
