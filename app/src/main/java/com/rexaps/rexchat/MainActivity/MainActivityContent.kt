package com.rexaps.rexchat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal fun __splitImpl_SessionListScreen(
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

    // ---------- create ----------

    if (showCreate) {
        var name by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New session") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        label = { Text("Name (optional)") },
                        placeholder = { Text("e.g. Personal") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "How do you want to link it?",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val s = SessionManager.create(ctx, name)
                            showCreate = false
                            refresh()
                            onOpen(s, "pair")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Phone, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Phone number (this phone)")
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val s = SessionManager.create(ctx, name)
                            showCreate = false
                            refresh()
                            onOpen(s, "qr")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCode2, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("QR code (another phone)")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Cancel") }
            }
        )
    }

    // ---------- rename ----------

    renaming?.let { s ->
        var name by remember(s.id) { mutableStateOf(s.name) }

        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename session") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        SessionManager.rename(ctx, s.id, name)
                        renaming = null
                        refresh()
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renaming = null }) { Text("Cancel") }
            }
        )
    }

    // ---------- delete ----------

    deleting?.let { s ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${s.name}?") },
            text = {
                Text("The saved login and all data for this session will be removed.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        SessionManager.delete(ctx, s.id)
                        deleting = null
                        refresh()
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancel") }
            }
        )
    }
}
