package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
internal fun RexMusicSheetsOverlay(
    state: RexMusicUiState,
    actions: RexActions,
    vm: RexMusicViewModel,
    showQueue: Boolean,
    onShowQueue: (Boolean) -> Unit,
    showRepeat: Boolean,
    onShowRepeat: (Boolean) -> Unit,
    menuTarget: MenuTarget?,
    onMenuTargetChange: (MenuTarget?) -> Unit,
    playlistTarget: RexTrack?,
    onPlaylistTargetChange: (RexTrack?) -> Unit,
    onShowCreatePlaylist: (Boolean) -> Unit
) {
    if (showQueue) {
        QueueSheet(
            state,
            actions
        ) {
            onShowQueue(false)
        }
    }

    if (showRepeat) {
        RepeatSheet(
            total = state.repeatTotal,
            left = state.repeatLeft,
            title = state.nowPlaying?.title.orEmpty(),
            onSet = {
                vm.setRepeat(it)
            },
            onDismiss = {
                onShowRepeat(false)
            }
        )
    }

    menuTarget?.let { target ->
        TrackMenuSheet(
            target,
            state,
            actions
        ) {
            onMenuTargetChange(null)
        }
    }

    playlistTarget?.let { target ->
        PlaylistPickerSheet(
            track = target,
            playlists = state.playlists,
            onPick = { name ->
                vm.addToPlaylist(name, target)
                onPlaylistTargetChange(null)
            },
            onCreate = {
                onPlaylistTargetChange(target)
                onShowCreatePlaylist(true)
            },
            onDismiss = { onPlaylistTargetChange(null) }
        )
    }
}
