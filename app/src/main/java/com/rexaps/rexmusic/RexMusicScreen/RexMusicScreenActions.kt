package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

internal fun createRexMusicActions(
    vm: RexMusicViewModel,
    state: RexMusicUiState,
    requestNotifications: () -> Unit,
    downloadTrack: (RexTrack) -> Unit,
    onShowPlayer: (Boolean) -> Unit,
    onDeleteTarget: (RexTrack?) -> Unit,
    onShowDeleteAll: (Boolean) -> Unit,
    onPendingDownload: (RexTrack?) -> Unit,
    onShowStorageDialog: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onTabChange: (Int) -> Unit,
    onLibraryTabChange: (Int) -> Unit,
    onMenuTargetChange: (MenuTarget?) -> Unit,
    onPlaylistTargetChange: (RexTrack?) -> Unit
): RexActions {
    return RexActions(
        playSearch = {
            requestNotifications()
            vm.playFromSearch(it)
            onShowPlayer(true)
        },

        playHistory = {
            requestNotifications()
            vm.playFromHistory(it)
            onShowPlayer(true)
        },

        playMain = {
            requestNotifications()
            vm.playFromMain(it)
            onShowPlayer(true)
        },

        playOffline = {
            requestNotifications()
            vm.playFromOffline(it)
            onShowPlayer(true)
        },

        playOfflineAll = { shuffle ->
            requestNotifications()
            vm.playOfflineAll(shuffle)
            onShowPlayer(true)
        },

        playMix = {
            requestNotifications()
            vm.playMix()
            onShowPlayer(true)
        },

        playQueue = {
            requestNotifications()
            vm.playFromQueue(it)
            onShowPlayer(true)
        },

        addQueue = {
            vm.addToQueue(it)
        },

        removeQueue = {
            vm.removeFromQueue(it)
        },

        moveQueueTop = {
            vm.moveQueueToTop(it)
        },

        clearQueue = {
            vm.clearQueue()
        },

        download = downloadTrack,

        cancelDownload = {
            vm.cancelDownload(it)
        },

        delete = {
            onDeleteTarget(it)
        },

        deleteAll = {
            onShowDeleteAll(true)
        },

        clearHistory = {
            vm.clearHistory()
        },

        requestAccess = {
            onPendingDownload(null)
            onShowStorageDialog(true)
        },

        setOffline = { on ->
            if (on != state.offline.enabled) {
                onQueryChange("")
                vm.setOfflineMode(on)
            }
        },

        toggleAutoplay = {
            vm.toggleAutoplay()
        },

        searchArtist = { name ->
            onTabChange(TAB_SEARCH)
            onQueryChange(name)

            if (!state.offline.enabled) {
                vm.search(name)
            }
        },

        goLibrary = {
            onTabChange(TAB_LIBRARY)
            onLibraryTabChange(0)
        },

        goPlaylists = {
            onTabChange(TAB_PLAYLIST)
        },

        openMenu = {
            onMenuTargetChange(it)
        },

        openPlaylistPicker = {
            onPlaylistTargetChange(it)
        },

        playPlaylist = {
            requestNotifications()
            vm.playPlaylist(it)
            onShowPlayer(true)
        },

        playPlaylistTrack = { playlistName, trackId ->
            requestNotifications()
            vm.playPlaylist(playlistName, trackId)
            onShowPlayer(true)
        },

        createPlaylist = { name ->
            vm.createPlaylist(name)
        },

        deletePlaylist = {
            vm.deletePlaylist(it)
        },

        dismissError = {
            vm.dismissError()
        }
    )
}
