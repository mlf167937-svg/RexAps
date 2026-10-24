package com.rexaps.rexchat

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SessionListDialogs(
    ctx: Context,
    showCreate: Boolean,
    onShowCreateChange: (Boolean) -> Unit,
    renaming: SessionManager.Session?,
    onRenamingChange: (SessionManager.Session?) -> Unit,
    deleting: SessionManager.Session?,
    onDeletingChange: (SessionManager.Session?) -> Unit,
    onOpen: (SessionManager.Session, String) -> Unit,
    onRefresh: () -> Unit
) {
    // ---------- create ----------

    if (showCreate) {
        var name by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { onShowCreateChange(false) },
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
                            onShowCreateChange(false)
                            onRefresh()
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
                            onShowCreateChange(false)
                            onRefresh()
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
                TextButton(onClick = { onShowCreateChange(false) }) { Text("Cancel") }
            }
        )
    }

    // ---------- rename ----------

    renaming?.let { s ->
        var name by remember(s.id) { mutableStateOf(s.name) }

        AlertDialog(
            onDismissRequest = { onRenamingChange(null) },
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
                        onRenamingChange(null)
                        onRefresh()
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { onRenamingChange(null) }) { Text("Cancel") }
            }
        )
    }

    // ---------- delete ----------

    deleting?.let { s ->
        AlertDialog(
            onDismissRequest = { onDeletingChange(null) },
            title = { Text("Delete ${s.name}?") },
            text = {
                Text("The saved login and all data for this session will be removed.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        SessionManager.delete(ctx, s.id)
                        onDeletingChange(null)
                        onRefresh()
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { onDeletingChange(null) }) { Text("Cancel") }
            }
        )
    }
}
