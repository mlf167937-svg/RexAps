package com.rexaps.rexchat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

// =====================================================================
// THEME
// =====================================================================

@Composable
fun RexTheme(content: @Composable () -> Unit) {
    val scheme =
        if (isSystemInDarkTheme()) {
            darkColorScheme(
                primary = Color(0xFF25D366),
                tertiary = Color(0xFF34B7F1)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF128C7E),
                tertiary = Color(0xFF075E54)
            )
        }

    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}

// =====================================================================
// OPEN SESSION (dipakai MainActivity dan route UI utama)
// =====================================================================

fun openRexChatSession(
    context: Context,
    session: SessionManager.Session,
    method: String = ""
) {
    val modern = SessionManager.profilesSupported()

    val intent = Intent(
        context,
        if (modern) WaActivity::class.java else WaLegacyActivity::class.java
    )
        .putExtra(WaActivity.EXTRA_SESSION_ID, session.id)
        .putExtra(WaActivity.EXTRA_METHOD, method)

    if (modern) {
        // Setiap session = task sendiri di Recents
        intent.data = Uri.parse("rexchat://session/${session.id}")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    }

    // Aman dipanggil dari Context non-Activity
    if (context !is android.app.Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    context.startActivity(intent)
}

// =====================================================================
// ACTIVITY (opsional, kalau mau dijalankan standalone)
// =====================================================================

class MainActivity : ComponentActivity() {

    private var tick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RexTheme {
                SessionListScreen(
                    refreshTick = tick,
                    onOpen = { session, method ->
                        openRexChatSession(this, session, method)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SessionManager.purgePending(this)
        tick++
    }
}

// =====================================================================
// SESSION LIST SCREEN
// =====================================================================

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
                .background(MaterialTheme.colorScheme.background)
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

// =====================================================================
// SESSION CARD
// =====================================================================

@Composable
private fun SessionCard(
    s: SessionManager.Session,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    s.id.toString(),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    s.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val status = if (s.linked) "Linked" else "Not linked"
                val last =
                    if (s.lastOpened > 0) {
                        " • " + DateUtils.getRelativeTimeSpanString(
                            s.lastOpened,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        )
                    } else ""

                Text(
                    status + last,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (s.linked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRename) {
                Icon(Icons.Default.Edit, "Rename")
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete")
            }
        }
    }
}