package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

@Composable
internal fun RexMusicSyncTwoPane(
    state: SyncUiState,
    offlineState: OfflineState,
    active: Boolean,
    connecting: Boolean,
    room: String,
    ip: String,
    password: String,
    name: String,
    query: String,
    onlineResults: List<RexTrack>,
    searching: Boolean,
    searchMessage: String,
    onRoomChange: (String) -> Unit,
    onIpChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onAutoIp: () -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    onSearchOnline: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
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
                    onRoomChange = onRoomChange,
                    ip = ip,
                    onIpChange = onIpChange,
                    password = password,
                    onPasswordChange = onPasswordChange,
                    name = name,
                    onNameChange = onNameChange,
                    active = active,
                    onAutoIp = onAutoIp,
                    onCreateRoom = onCreateRoom,
                    onJoinRoom = onJoinRoom
                )
            }
            item {
                SyncOnlineSearchCard(
                    query = query,
                    onQueryChange = onQueryChange,
                    searching = searching,
                    searchMessage = searchMessage,
                    onSearch = onSearchOnline
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
                offlineState.entries.distinctBy { it.track.id }.filter { it.track.id.isNotBlank() },
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
                items(state.members.distinctBy { it.id }, key = { "member-${it.id}" }) { member ->
                    SyncMemberCard(
                        member = member,
                        state = state,
                        onVolumeChange = { value -> RexMusicSyncEngine.setMemberVolume(member.id, value) }
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
                if (state.queue.distinctBy { it.id }.isEmpty()) item { SyncEmptyQueueCard() }
                items(state.queue.distinctBy { it.id }, key = { "queue-${it.id}" }) { track ->
                    SyncQueueTrackCard(track = track, onPlay = { RexMusicSyncEngine.playQueued(track) })
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
}
