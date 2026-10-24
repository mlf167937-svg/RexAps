package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.theme.Rex

@Composable
internal fun SshLoginDialog(s: RexCoderState) {
    var showPassword by rememberSaveable { mutableStateOf(false) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { s.sshLoginVisible = false },
        containerColor = Rex.Overlay,
        titleContentColor = Rex.TextBright,
        textContentColor = Rex.Text,
        title = { Text("RexCoder SSH") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Login SSH seperti RexPanel, tetapi terminal tetap berada di dalam RexCoder.", fontSize = 12.sp, color = Rex.TextDim)
                androidx.compose.material3.OutlinedTextField(
                    value = s.sshCommand,
                    onValueChange = { s.sshCommand = it },
                    singleLine = true,
                    label = { Text("SSH command") },
                    placeholder = { Text("ssh -p 8022 user@192.168.0.101") }
                )
                androidx.compose.material3.OutlinedTextField(
                    value = s.sshPassword,
                    onValueChange = { s.sshPassword = it },
                    singleLine = true,
                    label = { Text("Password") },
                    visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                if (showPassword) "Sembunyikan password" else "Tampilkan password"
                            )
                        }
                    }
                )
                // Auto `cd` into the RexCoder workspace right after a successful login.
                Row(
                    Modifier.fillMaxWidth()
                        .toggleable(value = s.sshAutoCd, role = Role.Checkbox, onValueChange = { s.sshAutoCd = it }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = s.sshAutoCd, onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = Rex.Accent, checkmarkColor = Rex.OnAccent, uncheckedColor = Rex.TextDim)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Auto cd ke workspace", fontSize = 13.sp, color = Rex.Text)
                        Text(com.rexaps.rexcoder.storage.WorkspaceManager.DISPLAY_PATH, fontSize = 11.sp, color = Rex.TextDim)
                    }
                }
                Text("Format: ssh -p PORT user@host", fontSize = 11.sp, color = Rex.TextDim)
            }
        },
        confirmButton = {
            TextButton(onClick = { s.connectSsh(s.sshCommand, s.sshPassword) }) { Text("Connect", color = Rex.Accent) }
        },
        dismissButton = {
            TextButton(onClick = { s.sshLoginVisible = false }) { Text("Cancel", color = Rex.TextDim) }
        }
    )
}
