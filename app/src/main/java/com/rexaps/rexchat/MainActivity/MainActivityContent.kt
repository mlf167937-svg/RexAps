package com.rexaps.rexchat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun SessionListScreen(
    refreshTick: Int = 0,
    onOpen: (SessionManager.Session, String) -> Unit,
    onExit: (() -> Unit)? = null
) {
    val ctx = LocalContext.current

    var sessions by remember { mutableStateOf(emptyList<SessionManager.Session>()) }
    var showCreate by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<SessionManager.Session?>(null) }
    var deleting by remember { mutableStateOf<SessionManager.Session?>(null) }

    fun refresh() {
        sessions = SessionManager.list(ctx)
    }

    // Refresh saat layar tampil dan tiap kembali dari layar WhatsApp
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, refreshTick) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                SessionManager.purgePending(ctx)
                refresh()
            }
        }
        owner.lifecycle.addObserver(observer)
        refresh()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    if (onExit != null) {
        BackHandler { onExit() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("New session") }
            )
        }
    ) { pad ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp)
        ) {

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onExit != null) {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }

                Column {
                    Text(
                        "RexChat",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        "Multiple WhatsApp Web sessions, each with its own login.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (sessions.isEmpty()) {

                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Chat,
                        null,
                        Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No sessions yet",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Tap New session to link a WhatsApp account.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            } else {

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(sessions, key = { it.id }) { s ->
                        SessionCard(
                            s = s,
                            onClick = { onOpen(s, "") },
                            onRename = { renaming = s },
                            onDelete = { deleting = s }
                        )
                    }
                }
            }
        }
    }

    SessionListDialogs(
        ctx = ctx,
        showCreate = showCreate,
        onShowCreateChange = { showCreate = it },
        renaming = renaming,
        onRenamingChange = { renaming = it },
        deleting = deleting,
        onDeletingChange = { deleting = it },
        onOpen = onOpen,
        onRefresh = ::refresh
    )
}

// =====================================================================
// SESSION CARD
// =====================================================================
