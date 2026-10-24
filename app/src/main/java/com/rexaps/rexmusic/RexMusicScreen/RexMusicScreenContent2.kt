package com.rexaps.rexmusic

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
internal fun RexMusicContent(onBack: () -> Unit, vm: RexMusicViewModel) {
    val context = LocalContext.current

    val state by remember(vm) {
        vm.state.map { it.copy(positionMs = 0L) }.distinctUntilChanged()
    }.collectAsState(initial = vm.state.value.copy(positionMs = 0L))

    val positionState = remember(vm) {
        vm.state.map { it.positionMs }.distinctUntilChanged()
    }.collectAsState(initial = vm.state.value.positionMs)

    var tab by rememberSaveable { mutableStateOf(TAB_HOME) }
    var libTab by rememberSaveable { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var typing by remember { mutableStateOf(false) }
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    var selectedPlaylistName by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDeletePlaylistName by rememberSaveable { mutableStateOf<String?>(null) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    var showRepeat by rememberSaveable { mutableStateOf(false) }
    // Permission/track-target dialogs are transient because their selected RexTrack is not saveable.
    var showStorageDialog by remember { mutableStateOf(false) }
    var showDeleteAll by rememberSaveable { mutableStateOf(false) }
    var pendingDownload by remember { mutableStateOf<RexTrack?>(null) }
    var deleteTarget by remember { mutableStateOf<RexTrack?>(null) }
    var menuTarget by remember { mutableStateOf<MenuTarget?>(null) }
    var playlistTarget by remember { mutableStateOf<RexTrack?>(null) }
    var showCreatePlaylist by remember { mutableStateOf(false) }

    // Preserve liked IDs across configuration changes without needing to serialize RexTrack objects.
    val likedIds = rememberSaveable(
        saver = listSaver(
            save = { ids -> ids.toList() },
            restore = { saved -> mutableStateListOf<String>().apply { addAll(saved) } }
        )
    ) { mutableStateListOf<String>() }
    val snackbar = remember { SnackbarHostState() }

    val homeList = rememberLazyListState()
    val searchList = rememberLazyListState()
    val playlistList = rememberLazyListState()
    val libraryList = rememberLazyListState()

    RexMusicScreenEffects(
        vm = vm,
        query = query,
        onTypingChange = { typing = it },
        snackbar = snackbar
    )

    val permissionActions = rememberRexMusicPermissionActions(
        vm = vm,
        state = state,
        pendingDownload = pendingDownload,
        onPendingDownload = { pendingDownload = it },
        onShowStorageDialog = { showStorageDialog = it }
    )
    val requestNotifications = permissionActions.requestNotifications
    val openStorageSettings = permissionActions.openStorageSettings
    val downloadTrack = permissionActions.downloadTrack

    BackHandler(enabled = showPlayer) {
        showPlayer = false
    }

    BackHandler(enabled = !showPlayer && tab == TAB_PLAYLIST && selectedPlaylistName != null) {
        selectedPlaylistName = null
    }

    BackHandler(enabled = !showPlayer && tab != TAB_HOME && !(tab == TAB_PLAYLIST && selectedPlaylistName != null)) {
        tab = TAB_HOME
        selectedPlaylistName = null
    }

    val actions = createRexMusicActions(
        vm = vm,
        state = state,
        requestNotifications = requestNotifications,
        downloadTrack = downloadTrack,
        onShowPlayer = { showPlayer = it },
        onDeleteTarget = { deleteTarget = it },
        onShowDeleteAll = { showDeleteAll = it },
        onPendingDownload = { pendingDownload = it },
        onShowStorageDialog = { showStorageDialog = it },
        onQueryChange = { query = it },
        onTabChange = { tab = it },
        onLibraryTabChange = { libTab = it },
        onMenuTargetChange = { menuTarget = it },
        onPlaylistTargetChange = { playlistTarget = it }
    )

    Box(Modifier.fillMaxSize()) {
        RexMusicScreenLayout(
            state = state,
            tab = tab,
            actions = actions,
            homeList = homeList,
            searchList = searchList,
            playlistList = playlistList,
            libraryList = libraryList,
            onBack = onBack,
            query = query,
            typing = typing,
            onQueryChange = { value ->
                query = value
                if (!state.offline.enabled) {
                    if (value.isBlank()) vm.clearSearch() else vm.search(value)
                }
            },
            onClearSearch = { query = ""; vm.clearSearch() },
            selectedPlaylistName = selectedPlaylistName,
            onOpenPlaylist = { selectedPlaylistName = it },
            onBackFromPlaylist = { selectedPlaylistName = null },
            onRequestDeletePlaylist = { pendingDeletePlaylistName = it },
            libTab = libTab,
            onLibTabChange = { libTab = it },
            onTabSelected = { nextTab ->
                tab = nextTab
                if (nextTab != TAB_PLAYLIST) selectedPlaylistName = null
            },
            onReturnHome = { tab = TAB_HOME },
            miniPlayer = {
                val now = state.nowPlaying
                AnimatedVisibility(
                    visible = now != null,
                    enter = fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                        slideInVertically(tween(250, easing = FastOutSlowInEasing)) { it / 3 },
                    exit = fadeOut(tween(130)) +
                        slideOutVertically(tween(180, easing = FastOutSlowInEasing)) { it / 4 }
                ) {
                    if (now != null) MiniPlayer(
                        track = now,
                        state = state,
                        positionState = positionState,
                        onToggle = { vm.togglePlay() },
                        onNext = { vm.next() },
                        onOpen = { showPlayer = true }
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbar) }
        )
        RexMusicPlayerOverlays(
            state = state,
            positionState = positionState,
            vm = vm,
            showPlayer = showPlayer,
            onShowPlayer = { showPlayer = it },
            likedIds = likedIds,
            onMenuTargetChange = { menuTarget = it },
            onShowRepeat = { showRepeat = it },
            onShowQueue = { showQueue = it },
            onDeleteTarget = { deleteTarget = it },
            downloadTrack = downloadTrack
        )
    }

    RexMusicSheetsOverlay(
        state = state, actions = actions, vm = vm,
        showQueue = showQueue, onShowQueue = { showQueue = it },
        showRepeat = showRepeat, onShowRepeat = { showRepeat = it },
        menuTarget = menuTarget, onMenuTargetChange = { menuTarget = it },
        playlistTarget = playlistTarget, onPlaylistTargetChange = { playlistTarget = it },
        onShowCreatePlaylist = { showCreatePlaylist = it }
    )
    RexMusicAlertDialogs(
        state = state, actions = actions, vm = vm,
        showCreatePlaylist = showCreatePlaylist, onShowCreatePlaylist = { showCreatePlaylist = it },
        pendingDeletePlaylistName = pendingDeletePlaylistName,
        onPendingDeletePlaylistName = { pendingDeletePlaylistName = it },
        selectedPlaylistName = selectedPlaylistName,
        onSelectedPlaylistName = { selectedPlaylistName = it },
        showStorageDialog = showStorageDialog, onShowStorageDialog = { showStorageDialog = it },
        onPendingDownload = { pendingDownload = it },
        deleteTarget = deleteTarget, onDeleteTarget = { deleteTarget = it },
        showDeleteAll = showDeleteAll, onShowDeleteAll = { showDeleteAll = it },
        openStorageSettings = openStorageSettings
    )

    RexMusicSheetsOverlay(
        state = state, actions = actions, vm = vm,
        showQueue = showQueue, onShowQueue = { showQueue = it },
        showRepeat = showRepeat, onShowRepeat = { showRepeat = it },
        menuTarget = menuTarget, onMenuTargetChange = { menuTarget = it },
        playlistTarget = playlistTarget, onPlaylistTargetChange = { playlistTarget = it },
        onShowCreatePlaylist = { showCreatePlaylist = it }
    )
    RexMusicAlertDialogs(
        state = state, actions = actions, vm = vm,
        showCreatePlaylist = showCreatePlaylist, onShowCreatePlaylist = { showCreatePlaylist = it },
        pendingDeletePlaylistName = pendingDeletePlaylistName,
        onPendingDeletePlaylistName = { pendingDeletePlaylistName = it },
        selectedPlaylistName = selectedPlaylistName,
        onSelectedPlaylistName = { selectedPlaylistName = it },
        showStorageDialog = showStorageDialog, onShowStorageDialog = { showStorageDialog = it },
        onPendingDownload = { pendingDownload = it },
        deleteTarget = deleteTarget, onDeleteTarget = { deleteTarget = it },
        showDeleteAll = showDeleteAll, onShowDeleteAll = { showDeleteAll = it },
        openStorageSettings = openStorageSettings
    )
}
