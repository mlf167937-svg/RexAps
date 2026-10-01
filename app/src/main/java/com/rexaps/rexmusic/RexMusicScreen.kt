package com.rexaps.rexmusic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rexmusic.player.MusicPlayerCard
import com.rexaps.rexmusic.player.PlayerActions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

private const val NOTIF_PERMISSION = "android.permission.POST_NOTIFICATIONS"

private const val TAB_HOME = 0
private const val TAB_SEARCH = 1
private const val TAB_LIBRARY = 2

@Composable
fun RexMusicScreen(
    onBack: () -> Unit = {},
    vm: RexMusicViewModel = viewModel()
) {
    RexMusicTheme {
        RexMusicContent(onBack, vm)
    }
}

@Composable
private fun RexMusicContent(onBack: () -> Unit, vm: RexMusicViewModel) {
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
    var showPlayer by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showRepeat by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showDeleteAll by remember { mutableStateOf(false) }
    var pendingDownload by remember { mutableStateOf<RexTrack?>(null) }
    var deleteTarget by remember { mutableStateOf<RexTrack?>(null) }
    var menuTarget by remember { mutableStateOf<MenuTarget?>(null) }

    val liked = remember { mutableStateMapOf<String, Boolean>() }
    val snackbar = remember { SnackbarHostState() }

    val homeList = rememberLazyListState()
    val searchList = rememberLazyListState()
    val libraryList = rememberLazyListState()

    LaunchedEffect(vm) {
        vm.refreshOffline()
    }

    LaunchedEffect(vm) {
        vm.state
            .map { it.notice }
            .distinctUntilChanged()
            .filterNotNull()
            .collectLatest { message ->
                vm.consumeNotice()
                snackbar.showSnackbar(message)
            }
    }

    LaunchedEffect(query) {
        typing = query.isNotBlank()

        if (typing) {
            delay(450)
            typing = false
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val requestNotifications: () -> Unit = {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                NOTIF_PERMISSION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(NOTIF_PERMISSION)
        }
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        vm.refreshOffline()
    }

    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        vm.refreshOffline()
    }

    val openStorageSettings: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appIntent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )

            try {
                settingsLauncher.launch(appIntent)
            } catch (_: Exception) {
                settingsLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                )
            }
        } else {
            storageLauncher.launch(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    LaunchedEffect(state.offline.hasAccess) {
        val pending = pendingDownload

        if (state.offline.hasAccess && pending != null) {
            pendingDownload = null
            showStorageDialog = false
            vm.download(pending)
        }
    }

    val downloadTrack: (RexTrack) -> Unit = { track ->
        if (RexOfflineManager.hasAccess(context)) {
            vm.download(track)
        } else {
            pendingDownload = track
            showStorageDialog = true
        }
    }

    BackHandler(enabled = showPlayer) {
        showPlayer = false
    }

    BackHandler(enabled = !showPlayer && tab != TAB_HOME) {
        tab = TAB_HOME
    }

    val actions = RexActions(
        playSearch = {
            requestNotifications()
            vm.playFromSearch(it)
            showPlayer = true
        },

        playHistory = {
            requestNotifications()
            vm.playFromHistory(it)
            showPlayer = true
        },

        playMain = {
            requestNotifications()
            vm.playFromMain(it)
            showPlayer = true
        },

        playOffline = {
            requestNotifications()
            vm.playFromOffline(it)
            showPlayer = true
        },

        playOfflineAll = { shuffle ->
            requestNotifications()
            vm.playOfflineAll(shuffle)
            showPlayer = true
        },

        playMix = {
            requestNotifications()
            vm.playMix()
            showPlayer = true
        },

        playQueue = {
            requestNotifications()
            vm.playFromQueue(it)
            showPlayer = true
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
            deleteTarget = it
        },

        deleteAll = {
            showDeleteAll = true
        },

        clearHistory = {
            vm.clearHistory()
        },

        requestAccess = {
            pendingDownload = null
            showStorageDialog = true
        },

        setOffline = { on ->
            if (on != state.offline.enabled) {
                query = ""
                vm.setOfflineMode(on)
            }
        },

        toggleAutoplay = {
            vm.toggleAutoplay()
        },

        searchArtist = { name ->
            tab = TAB_SEARCH
            query = name

            if (!state.offline.enabled) {
                vm.search(name)
            }
        },

        goLibrary = {
            tab = TAB_LIBRARY
            libTab = 0
        },

        openMenu = {
            menuTarget = it
        },

        dismissError = {
            vm.dismissError()
        }
    )

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,

            snackbarHost = {
                SnackbarHost(snackbar)
            },

            bottomBar = {
                Column {
                    val now = state.nowPlaying

                    if (now != null) {
                        MiniPlayer(
                            track = now,
                            state = state,
                            positionState = positionState,
                            onToggle = {
                                vm.togglePlay()
                            },
                            onNext = {
                                vm.next()
                            },
                            onOpen = {
                                showPlayer = true
                            }
                        )
                    }

                    NavigationBar {
                        NavigationBarItem(
                            selected = tab == TAB_HOME,
                            onClick = {
                                tab = TAB_HOME
                            },
                            icon = {
                                Icon(
                                    Icons.Default.Home,
                                    contentDescription = null
                                )
                            },
                            label = {
                                Text("Beranda")
                            }
                        )

                        NavigationBarItem(
                            selected = tab == TAB_SEARCH,
                            onClick = {
                                tab = TAB_SEARCH
                            },
                            icon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null
                                )
                            },
                            label = {
                                Text("Cari")
                            }
                        )

                        NavigationBarItem(
                            selected = tab == TAB_LIBRARY,
                            onClick = {
                                tab = TAB_LIBRARY
                            },
                            icon = {
                                Icon(
                                    Icons.Default.LibraryMusic,
                                    contentDescription = null
                                )
                            },
                            label = {
                                Text("Koleksi")
                            }
                        )
                    }
                }
            }
        ) { pad ->

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pad)
            ) {
                when (tab) {
                    TAB_HOME -> {
                        HomeTab(
                            state,
                            actions,
                            homeList,
                            onBack
                        )
                    }

                    TAB_SEARCH -> {
                        SearchTab(
                            state = state,
                            query = query,
                            typing = typing && !state.offline.enabled,

                            onQueryChange = {
                                query = it

                                if (!state.offline.enabled) {
                                    if (it.isBlank()) {
                                        vm.clearSearch()
                                    } else {
                                        vm.search(it)
                                    }
                                }
                            },

                            onClear = {
                                query = ""
                                vm.clearSearch()
                            },

                            actions = actions,
                            listState = searchList
                        )
                    }

                    else -> {
                        LibraryTab(
                            state,
                            libTab,
                            { libTab = it },
                            actions,
                            libraryList
                        )
                    }
                }

                ErrorCard(
                    error = state.error,
                    onDismiss = actions.dismissError,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        AnimatedVisibility(
            visible = showPlayer,
            enter = slideInVertically(tween(280)) { it } +
                fadeIn(tween(200)),
            exit = slideOutVertically(tween(220)) { it } +
                fadeOut(tween(160))
        ) {
            val track = state.nowPlaying

            if (track != null) {
                var accent by remember(track.cover) {
                    mutableStateOf<Color?>(null)
                }

                val dlKey = remember(track.title, track.artist) {
                    offlineKey(track)
                }

                MusicPlayerCard(
                    track = track,
                    state = state,
                    positionState = positionState,
                    isLiked = liked[track.id] == true,
                    downloaded = state.offline.isDownloaded(
                        track,
                        dlKey
                    ),
                    downloadStatus = state.offline.downloads[dlKey],
                    accent = accent,
                    onAccentFound = {
                        accent = it
                    },

                    actions = PlayerActions(
                        onClose = {
                            showPlayer = false
                        },

                        onLyrics = {
                            vm.toggleLyrics()
                        },

                        onMore = {
                            menuTarget = MenuTarget(track, null)
                        },

                        onPlayPause = {
                            vm.togglePlay()
                        },

                        onNext = {
                            vm.next()
                        },

                        onPrev = {
                            vm.prev()
                        },

                        onOpenPrevious = {
                            vm.prev(force = true)
                        },

                        onSeek = {
                            vm.seekTo(it)
                        },

                        onToggleLike = {
                            liked[track.id] = liked[track.id] != true
                        },

                        onRepeat = {
                            showRepeat = true
                        },

                        onAutoplay = {
                            vm.toggleAutoplay()
                        },

                        onQueue = {
                            showQueue = true
                        },

                        onDownload = {
                            downloadTrack(track)
                        },

                        onCancelDownload = {
                            vm.cancelDownload(track)
                        },

                        onDelete = {
                            deleteTarget = track
                        }
                    )
                )
            } else {
                PlayerPlaceholder(
                    state.loadingText,
                    state.error
                ) {
                    showPlayer = false
                }
            }
        }

        AnimatedVisibility(
            visible = state.lyricsVisible,
            enter = fadeIn(tween(240)),
            exit = fadeOut(tween(180))
        ) {
            RexLyricsOverlay(
                track = state.nowPlaying
                    ?: RexTrack(
                        id = "",
                        title = "",
                        artist = ""
                    ),
                lyrics = state.lyrics,
                loading = state.lyricsLoading,
                error = state.lyricsError,
                accent = null,
                positionState = positionState,
                durationMs = state.durationMs,
                onClose = {
                    vm.closeLyrics()
                }
            )
        }
    }

    if (showQueue) {
        QueueSheet(
            state,
            actions
        ) {
            showQueue = false
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
                showRepeat = false
            }
        )
    }

    menuTarget?.let { target ->
        TrackMenuSheet(
            target,
            state,
            actions
        ) {
            menuTarget = null
        }
    }

    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = {
                showStorageDialog = false
                pendingDownload = null
            },

            icon = {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null
                )
            },

            title = {
                Text("Izinkan akses penyimpanan")
            },

            text = {
                Text(
                    "Lagu yang diunduh disimpan di " +
                        "${RexOfflineManager.DISPLAY_PATH}/ " +
                        "supaya mudah kamu buka dan hapus lewat aplikasi File. " +
                        "Aplikasi butuh izin akses penyimpanan untuk itu."
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        openStorageSettings()
                    }
                ) {
                    Text("Buka pengaturan")
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showStorageDialog = false
                        pendingDownload = null
                    }
                ) {
                    Text("Nanti")
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = {
                deleteTarget = null
            },

            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null
                )
            },

            title = {
                Text("Hapus dari offline?")
            },

            text = {
                Text(
                    "\"${target.title}\" akan dihapus dari " +
                        "${RexOfflineManager.DISPLAY_PATH}/. " +
                        "Kamu tetap bisa memutarnya lagi saat online."
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteDownload(target)
                        deleteTarget = null
                    }
                ) {
                    Text(
                        "Hapus",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        deleteTarget = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = {
                showDeleteAll = false
            },

            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null
                )
            },

            title = {
                Text("Hapus semua lagu offline?")
            },

            text = {
                Text(
                    "${state.offline.entries.size} lagu " +
                        "(${formatBytes(state.offline.totalBytes)}) " +
                        "akan dihapus dari perangkat."
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteAllDownloads()
                        showDeleteAll = false
                    }
                ) {
                    Text(
                        "Hapus semua",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteAll = false
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun PlayerPlaceholder(
    loadingText: String,
    error: String?,
    onClose: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(RexPlayerBase)
            .clickable(
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = null,
                onClick = {}
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        RexIconButton(
            Icons.Default.KeyboardArrowDown,
            "Tutup player",
            onClose,
            Modifier.padding(4.dp),
            size = 48.dp,
            iconSize = 32.dp,
            tint = Color.White
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (error.isNullOrBlank()) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(44.dp)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    loadingText.ifBlank { "Memuat..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            } else {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = Color.White
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MiniPlayer(
    track: RexTrack,
    state: RexMusicUiState,
    positionState: State<Long>,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val busy = state.phase.isPlayerBusy

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.surfaceVariant)
            .clickable(onClick = onOpen)
    ) {
        Row(
            Modifier.padding(
                start = 8.dp,
                end = 4.dp,
                top = 8.dp,
                bottom = 8.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoverThumb(
                track.cover,
                44.dp
            )

            Spacer(Modifier.width(12.dp))

            Column(
                Modifier.weight(1f)
            ) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (busy) {
                Box(
                    Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                RexIconButton(
                    if (state.isPlaying) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },

                    if (state.isPlaying) {
                        "Jeda"
                    } else {
                        "Putar"
                    },

                    onToggle
                )
            }

            RexIconButton(
                Icons.Default.SkipNext,
                "Lagu berikutnya",
                onNext
            )
        }

        MiniProgress(
            positionState,
            state.durationMs
        )
    }
}

@Composable
private fun MiniProgress(
    positionState: State<Long>,
    durationMs: Long
) {
    val scheme = MaterialTheme.colorScheme

    val progress = if (durationMs > 0) {
        (
            positionState.value.toFloat() / durationMs
        ).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(
                scheme.onSurface.copy(alpha = 0.1f)
            )
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(scheme.primary)
        )
    }
}
