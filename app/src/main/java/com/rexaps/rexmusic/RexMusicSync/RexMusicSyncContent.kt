package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RexMusicSyncScreenContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val state by RexMusicSyncEngine.state.collectAsState()
    val offlineState by RexOfflineManager.state.collectAsState()

    var room by rememberSaveable { mutableStateOf("RexSyncRoom1") }
    var ip by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("rexsync") }
    var name by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var onlineResults by remember { mutableStateOf<List<RexTrack>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchMessage by rememberSaveable { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val active = state.hosting || state.connected
    val connecting = !active &&
        state.error == null &&
        state.status.lowercase().let {
            "menghubungkan" in it || "connecting" in it || "mencari host" in it
        }

    LaunchedEffect(Unit) {
        RexMusicSyncEngine.bindContext(context)
        if (ip.isBlank()) ip = RexMusicSyncEngine.detectIp(context)
    }

    val searchOnline: () -> Unit = {
        if (!searching && query.isNotBlank()) {
            searching = true
            searchMessage = "Mencari lagu…"
            scope.launch {
                val result = RexMusicApi.search(query.trim())
                onlineResults = result.getOrElse { emptyList() }
                searchMessage = if (result.isSuccess) {
                    "${onlineResults.size} hasil · unduh dulu agar bisa dipakai di room"
                } else {
                    result.exceptionOrNull()?.message ?: "Pencarian gagal"
                }
                searching = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("RexMusicSync", fontWeight = FontWeight.Bold)
                        Text(
                            "Listen together · local Wi-Fi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val useTwoPaneLayout = maxWidth >= 680.dp && maxWidth > maxHeight

            if (useTwoPaneLayout) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                    ) {
                        item { SyncOverviewCard(state = state, active = active, connecting = connecting) }
                        item {
                            SyncConnectionCard(
                                room = room,
                                onRoomChange = { room = it },
                                ip = ip,
                                onIpChange = { ip = it },
                                password = password,
                                onPasswordChange = { password = it },
                                name = name,
                                onNameChange = { name = it },
                                active = active,
                                onAutoIp = { ip = RexMusicSyncEngine.detectIp(context) },
                                onCreateRoom = { RexMusicSyncEngine.createRoom(room, password, ip) },
                                onJoinRoom = {
                                    RexMusicSyncEngine.joinRoom(
                                        room.trim(),
                                        name.ifBlank { "User" }.trim(),
                                        ip.trim(),
                                        password
                                    )
                                }
                            )
                        }
                        item {
                            SyncOnlineSearchCard(
                                query = query,
                                onQueryChange = { query = it },
                                searching = searching,
                                searchMessage = searchMessage,
                                onSearch = searchOnline
                            )
                        }
                        items(
                            onlineResults.distinctBy { it.id }.filter { it.id.isNotBlank() },
                            key = { "search-${it.id}" }
                        ) { track ->
                            SyncOnlineResultCard(
                                track = track,
                                downloaded = offlineState.entries.any { it.track.id == track.id },
                                onDownload = { RexOfflineManager.download(track) }
                            )
                        }
                        item {
                            SyncOfflineHeader(
                                count = offlineState.entries.distinctBy { it.track.id }.size,
                                active = active
                            )
                        }
                        if (offlineState.entries.distinctBy { it.track.id }.none { it.track.id.isNotBlank() }) {
                            item { SyncEmptyOfflineCard() }
                        }
                        items(
                            offlineState.entries.distinctBy { it.track.id }
                                .filter { it.track.id.isNotBlank() },
                            key = { "offline-${it.track.id}" }
                        ) { entry ->
                            SyncOfflineTrackCard(
                                track = entry.track,
                                active = active,
                                onQueue = { RexMusicSyncEngine.addToQueue(entry.track) }
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                    ) {
                        if (active) {
                            item { SyncMembersHeader(state.members.distinctBy { it.id }.size) }
                            items(
                                state.members.distinctBy { it.id },
                                key = { "member-${it.id}" }
                            ) { member ->
                                SyncMemberCard(
                                    member = member,
                                    state = state,
                                    onVolumeChange = { value ->
                                        RexMusicSyncEngine.setMemberVolume(member.id, value)
                                    }
                                )
                            }
                            item {
                                SyncPlaybackCard(
                                    state = state,
                                    onHostVolumeChange = { RexMusicSyncEngine.setHostVolume(it) },
                                    onTogglePlayback = { RexMusicSyncEngine.togglePlayback() },
                                    onStop = { RexMusicSyncEngine.stopAll() }
                                )
                            }
                            item { SyncQueueHeader(state.queue.size) }
                            if (state.queue.distinctBy { it.id }.isEmpty()) {
                                item { SyncEmptyQueueCard() }
                            }
                            items(
                                state.queue.distinctBy { it.id },
                                key = { "queue-${it.id}" }
                            ) { track ->
                                SyncQueueTrackCard(
                                    track = track,
                                    onPlay = { RexMusicSyncEngine.playQueued(track) }
                                )
                            }
                            item {
                                OutlinedButton(
                                    onClick = { RexMusicSyncEngine.disconnect() },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(if (state.hosting) "Tutup room" else "Keluar dari room")
                                }
                            }
                        } else {
                            item { SyncWaitingCard(connecting = connecting, error = state.error) }
                            item { SyncMembersEmptyCard() }
                            item { SyncPlaybackEmptyCard() }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 30.dp)
                ) {
                    item { SyncOverviewCard(state = state, active = active, connecting = connecting) }
                    item {
                        SyncConnectionCard(
                            room = room,
                            onRoomChange = { room = it },
                            ip = ip,
                            onIpChange = { ip = it },
                            password = password,
                            onPasswordChange = { password = it },
                            name = name,
                            onNameChange = { name = it },
                            active = active,
                            onAutoIp = { ip = RexMusicSyncEngine.detectIp(context) },
                            onCreateRoom = { RexMusicSyncEngine.createRoom(room, password, ip) },
                            onJoinRoom = {
                                RexMusicSyncEngine.joinRoom(
                                    room,
                                    name.ifBlank { "User" },
                                    ip,
                                    password
                                )
                            }
                        )
                    }

                    if (active) {
                        item { SyncMembersHeader(state.members.distinctBy { it.id }.size) }
                        items(
                            state.members.distinctBy { it.id },
                            key = { "member-${it.id}" }
                        ) { member ->
                            SyncMemberCard(
                                member = member,
                                state = state,
                                onVolumeChange = { value ->
                                    RexMusicSyncEngine.setMemberVolume(member.id, value)
                                }
                            )
                        }
                        item {
                            SyncPlaybackCard(
                                state = state,
                                onHostVolumeChange = { RexMusicSyncEngine.setHostVolume(it) },
                                onTogglePlayback = { RexMusicSyncEngine.togglePlayback() },
                                onStop = { RexMusicSyncEngine.stopAll() }
                            )
                        }
                        item { SyncQueueHeader(state.queue.size) }
                        if (state.queue.distinctBy { it.id }.isEmpty()) {
                            item { SyncEmptyQueueCard() }
                        }
                        items(
                            state.queue.distinctBy { it.id },
                            key = { "queue-${it.id}" }
                        ) { track ->
                            SyncQueueTrackCard(
                                track = track,
                                onPlay = { RexMusicSyncEngine.playQueued(track) }
                            )
                        }
                        item {
                            OutlinedButton(
                                onClick = { RexMusicSyncEngine.disconnect() },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(if (state.hosting) "Tutup room" else "Keluar dari room")
                            }
                        }
                    } else {
                        item { SyncWaitingCard(connecting = connecting, error = state.error) }
                        item { SyncMembersEmptyCard() }
                        item { SyncPlaybackEmptyCard() }
                    }

                    item {
                        SyncOnlineSearchCard(
                            query = query,
                            onQueryChange = { query = it },
                            searching = searching,
                            searchMessage = searchMessage,
                            onSearch = searchOnline
                        )
                    }
                    items(
                        onlineResults.distinctBy { it.id }.filter { it.id.isNotBlank() },
                        key = { "search-${it.id}" }
                    ) { track ->
                        SyncOnlineResultCard(
                            track = track,
                            downloaded = offlineState.entries.any { it.track.id == track.id },
                            onDownload = { RexOfflineManager.download(track) }
                        )
                    }
                    item {
                        SyncOfflineHeader(
                            count = offlineState.entries.distinctBy { it.track.id }.size,
                            active = active
                        )
                    }
                    if (offlineState.entries.distinctBy { it.track.id }.none { it.track.id.isNotBlank() }) {
                        item { SyncEmptyOfflineCard() }
                    }
                    items(
                        offlineState.entries.distinctBy { it.track.id }
                            .filter { it.track.id.isNotBlank() },
                        key = { "offline-${it.track.id}" }
                    ) { entry ->
                        SyncOfflineTrackCard(
                            track = entry.track,
                            active = active,
                            onQueue = { RexMusicSyncEngine.addToQueue(entry.track) }
                        )
                    }
                }
            }
        }
    }
}
