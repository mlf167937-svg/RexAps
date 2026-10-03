package com.rexaps.rexchat

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rexaps.rexchat.rexchat.RexChatActivity

class MainActivity : ComponentActivity() {

    /*
     * Permission Android lama untuk membuat folder
     * pada public Download.
     */
    private val storagePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            /*
             * UI akan tetap bekerja walaupun permission
             * tidak diberikan. Pada Android modern,
             * metadata dapat ditangani oleh scoped storage.
             */
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        /*
         * Android <= 9.
         */
        if (
            android.os.Build.VERSION.SDK_INT <=
            android.os.Build.VERSION_CODES.P
        ) {

            runCatching {

                storagePermissionLauncher.launch(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }

        setContent {

            RexChatSessionScreen(
                onOpenSession = {
                    openSession(it)
                }
            )
        }
    }

    /**
     * Buka WebView process untuk session tertentu.
     */
    private fun openSession(
        session: SessionManager.Session
    ) {

        val intent =
            Intent(
                this,
                RexChatActivity::class.java
            ).apply {

                putExtra(
                    RexChatActivity.EXTRA_SESSION_ID,
                    session.id
                )
            }

        startActivity(
            intent
        )
    }
}

@androidx.compose.runtime.Composable
private fun RexChatSessionScreen(
    onOpenSession:
        (SessionManager.Session) -> Unit
) {

    var sessions by remember {
        mutableStateOf(
            emptyList<SessionManager.Session>()
        )
    }

    var showCreate by remember {
        mutableStateOf(false)
    }

    var editSession by remember {
        mutableStateOf<
            SessionManager.Session?
        >(null)
    }

    var deleteSession by remember {
        mutableStateOf<
            SessionManager.Session?
        >(null)
    }

    /*
     * Load daftar.
     */
    fun refresh() {

        sessions =
            SessionManager.getSessions(
                context =
                    androidx.compose.ui.platform
                        .LocalContext.current
            )
    }

    LaunchedEffect(
        Unit
    ) {

        refresh()
    }

    val context =
        androidx.compose.ui.platform
            .LocalContext.current

    /*
     * =========================================================
     * SCREEN
     * =========================================================
     */

    Scaffold(

        floatingActionButton = {

            androidx.compose.material3.FloatingActionButton(
                onClick = {
                    showCreate = true
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Add,
                    contentDescription =
                        "Tambah session"
                )
            }
        }

    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme
                        .colorScheme
                        .background
                )
                .padding(
                    paddingValues
                )
                .padding(
                    horizontal = 16.dp
                )
        ) {

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            /*
             * =================================================
             * HEADER
             * =================================================
             */

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(
                            RoundedCornerShape(
                                16.dp
                            )
                        )
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme
                                        .colorScheme
                                        .primary,

                                    MaterialTheme
                                        .colorScheme
                                        .tertiary
                                )
                            )
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Chat,
                        contentDescription =
                            null,
                        tint =
                            MaterialTheme
                                .colorScheme
                                .onPrimary,
                        modifier =
                            Modifier.size(28.dp)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.size(14.dp)
                )

                Column {

                    Text(
                        text =
                            "RexChat",
                        style =
                            MaterialTheme
                                .typography
                                .headlineSmall
                    )

                    Text(
                        text =
                            "WhatsApp Web Sessions",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            /*
             * =================================================
             * INFO
             * =================================================
             */

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(
                            16.dp
                        )
                ) {

                    Text(
                        text =
                            "Session tersimpan terpisah",
                        style =
                            MaterialTheme
                                .typography
                                .titleSmall
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "Setiap session memiliki data " +
                                    "WebView sendiri. Jadi login " +
                                    "Session 1 tidak bercampur " +
                                    "dengan Session 2.",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            /*
             * =================================================
             * EMPTY
             * =================================================
             */

            if (
                sessions.isEmpty()
            ) {

                Column(
                    modifier =
                        Modifier.fillMaxSize(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Chat,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(64.dp),
                        tint =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Text(
                        text =
                            "Belum ada session"
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "Tekan + untuk membuat " +
                                    "session WhatsApp baru.",
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )

                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )

                    Button(
                        onClick = {
                            showCreate = true
                        }
                    ) {

                        Text(
                            "Buat session"
                        )
                    }
                }

            } else {

                /*
                 * =================================================
                 * LIST
                 * =================================================
                 */

                LazyColumn(
                    modifier =
                        Modifier.fillMaxSize(),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                ) {

                    items(
                        items =
                            sessions,
                        key = {
                            it.id
                        }
                    ) { session ->

                        SessionCard(
                            session =
                                session,

                            onClick = {
                                onOpenSession(
                                    session
                                )
                            },

                            onRename = {
                                editSession =
                                    session
                            },

                            onDelete = {
                                deleteSession =
                                    session
                            }
                        )
                    }
                }
            }
        }
    }

    /*
     * =========================================================
     * CREATE
     * =========================================================
     */

    if (
        showCreate
    ) {

        CreateSessionDialog(

            onDismiss = {
                showCreate = false
            },

            onCreate = { name ->

                SessionManager.createSession(
                    context =
                        context,
                    name =
                        name
                )

                showCreate = false

                sessions =
                    SessionManager.getSessions(
                        context
                    )
            }
        )
    }

    /*
     * =========================================================
     * EDIT
     * =========================================================
     */

    editSession?.let { session ->

        RenameSessionDialog(

            session =
                session,

            onDismiss = {
                editSession = null
            },

            onRename = { name ->

                SessionManager.renameSession(
                    context =
                        context,
                    id =
                        session.id,
                    newName =
                        name
                )

                editSession = null

                sessions =
                    SessionManager.getSessions(
                        context
                    )
            }
        )
    }

    /*
     * =========================================================
     * DELETE
     * =========================================================
     */

    deleteSession?.let { session ->

        AlertDialog(

            onDismissRequest = {
                deleteSession = null
            },

            title = {
                Text(
                    "Hapus ${session.name}?"
                )
            },

            text = {
                Text(
                    "Folder metadata Session ${session.id} " +
                            "akan dihapus."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        SessionManager.deleteSession(
                            context =
                                context,
                            id =
                                session.id
                        )

                        deleteSession =
                            null

                        sessions =
                            SessionManager.getSessions(
                                context
                            )
                    }
                ) {

                    Text(
                        "Hapus"
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        deleteSession = null
                    }
                ) {

                    Text(
                        "Batal"
                    )
                }
            }
        )
    }
}

@androidx.compose.runtime.Composable
private fun SessionCard(
    session: SessionManager.Session,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        14.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            /*
             * ICON
             */

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(
                        CircleShape
                    )
                    .background(
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        session.id.toString(),
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onPrimaryContainer
                )
            }

            Spacer(
                modifier =
                    Modifier.size(14.dp)
            )

            /*
             * NAME
             */

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        session.name,
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Text(
                    text =
                        "Session ${session.id}",
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            /*
             * EDIT
             */

            IconButton(
                onClick = onRename
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Edit,
                    contentDescription =
                        "Rename"
                )
            }

            /*
             * DELETE
             */

            IconButton(
                onClick = onDelete
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Delete,
                    contentDescription =
                        "Hapus"
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun CreateSessionDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {

    var name by remember {
        mutableStateOf("")
    }

    AlertDialog(

        onDismissRequest =
            onDismiss,

        title = {
            Text(
                "Session baru"
            )
        },

        text = {

            Column {

                Text(
                    text =
                        "Buat session WhatsApp Web " +
                                "yang terpisah."
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                OutlinedTextField(
                    value =
                        name,
                    onValueChange = {
                        name = it
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text(
                            "Nama session"
                        )
                    },
                    placeholder = {
                        Text(
                            "Contoh: WhatsApp Utama"
                        )
                    }
                )
            }
        },

        confirmButton = {

            TextButton(
                onClick = {

                    onCreate(
                        if (
                            name.isBlank()
                        ) {
                            "Session"
                        } else {
                            name
                        }
                    )
                }
            ) {

                Text(
                    "Buat"
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(
                    "Batal"
                )
            }
        }
    )
}

@androidx.compose.runtime.Composable
private fun RenameSessionDialog(
    session: SessionManager.Session,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {

    var name by remember {
        mutableStateOf(
            session.name
        )
    }

    AlertDialog(

        onDismissRequest =
            onDismiss,

        title = {
            Text(
                "Rename session"
            )
        },

        text = {

            OutlinedTextField(
                value =
                    name,
                onValueChange = {
                    name = it
                },
                modifier =
                    Modifier.fillMaxWidth(),
                singleLine = true,
                label = {
                    Text(
                        "Nama"
                    )
                }
            )
        },

        confirmButton = {

            TextButton(
                onClick = {

                    if (
                        name.isNotBlank()
                    ) {

                        onRename(
                            name
                        )
                    }
                }
            ) {

                Text(
                    "Simpan"
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(
                    "Batal"
                )
            }
        }
    )
}
