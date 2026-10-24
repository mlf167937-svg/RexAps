package com.rexaps.rexmusic.player

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.rexaps.rexmusic.*

//  Full-screen player 

@Composable
fun MusicPlayerCard(
    track: RexTrack,
    state: RexMusicUiState,
    positionState: State<Long>,
    isLiked: Boolean,
    downloaded: Boolean,
    downloadStatus: DownloadStatus?,
    accent: Color?,
    onAccentFound: (Color) -> Unit,
    actions: PlayerActions,
    modifier: Modifier = Modifier
) {
    MusicPlayerCardContent(
        track = track,
        state = state,
        positionState = positionState,
        isLiked = isLiked,
        downloaded = downloaded,
        downloadStatus = downloadStatus,
        accent = accent,
        onAccentFound = onAccentFound,
        actions = actions,
        modifier = modifier
    )
}
