package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
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
@Composable internal
fun __RexMusicSyncScreenContent(onBack: () -> Unit) {
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
                RexMusicSyncTwoPane(
                    state = state, offlineState = offlineState, active = active, connecting = connecting,
                    room = room, ip = ip, password = password, name = name, query = query,
                    onlineResults = onlineResults, searching = searching, searchMessage = searchMessage,
                    onRoomChange = { room = it }, onIpChange = { ip = it },
                    onPasswordChange = { password = it }, onNameChange = { name = it },
                    onQueryChange = { query = it },
                    onAutoIp = { ip = RexMusicSyncEngine.detectIp(context) },
                    onCreateRoom = { RexMusicSyncEngine.createRoom(room, password, ip) },
                    onJoinRoom = { RexMusicSyncEngine.joinRoom(room.trim(), name.ifBlank { "User" }.trim(), ip.trim(), password) },
                    onSearchOnline = searchOnline
                )
            } else {
                RexMusicSyncSinglePane(
                    state = state, offlineState = offlineState, active = active, connecting = connecting,
                    room = room, ip = ip, password = password, name = name, query = query,
                    onlineResults = onlineResults, searching = searching, searchMessage = searchMessage,
                    onRoomChange = { room = it }, onIpChange = { ip = it },
                    onPasswordChange = { password = it }, onNameChange = { name = it },
                    onQueryChange = { query = it },
                    onAutoIp = { ip = RexMusicSyncEngine.detectIp(context) },
                    onCreateRoom = { RexMusicSyncEngine.createRoom(room, password, ip) },
                    onJoinRoom = { RexMusicSyncEngine.joinRoom(room, name.ifBlank { "User" }, ip, password) },
                    onSearchOnline = searchOnline
                )
            }
        }
    }
}
