@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_PushDialog(
    message: String,
    onMessageChange: (String) -> Unit,
    type: RexGitCommitType,
    onTypeChange: (RexGitCommitType) -> Unit,
    folder: String,
    folders: List<String>,
    packagePath: String,
    useGitDev: Boolean,
    gitDevAvailable: Boolean,
    onUseGitDevChange: (Boolean) -> Unit,
    onFolderChange: (String) -> Unit,
    bumpVersion: Boolean,
    onBumpChange: (Boolean) -> Unit,
    changeCount: Int,
    onDismiss: () -> Unit,
    onPush: () -> Unit
) {
    var typeMenuOpen by remember { mutableStateOf(false) }
    var folderMenuOpen by remember { mutableStateOf(false) }

    val canCommit = changeCount == 0 || (
        message.isNotBlank() && (!useGitDev || (folder.isNotBlank() && folders.contains(folder)))
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Commit & push") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (gitDevAvailable) {
                    Row(
                        Modifier.fillMaxWidth().toggleable(
                            value = useGitDev,
                            role = Role.Switch,
                            onValueChange = onUseGitDevChange
                        ).heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("GitDev metadata", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (useGitDev) "Version prefix and package folder scope enabled" else "Standard commit message; no folder required",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = useGitDev, onCheckedChange = null)
                    }
                } else {
                    Text(
                        "Standard Push is ready. Configure GitDev only for repositories that need versioning and folder metadata.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    if (changeCount == 0) {
                        "No local changes. Existing commits will be pushed."
                    } else {
                        "$changeCount changed file(s) will be committed and pushed to origin."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                if (changeCount > 0) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { typeMenuOpen = true },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text(type.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            DropdownMenu(
                                expanded = typeMenuOpen,
                                onDismissRequest = { typeMenuOpen = false }
                            ) {
                                RexGitCommitType.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text("${option.label} · ${option.description}") },
                                        onClick = {
                                            onTypeChange(option)
                                            typeMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        if (useGitDev) {
                            Box(Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { folderMenuOpen = true },
                                    enabled = folders.isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text(folder.ifBlank { "Folder" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                DropdownMenu(
                                    expanded = folderMenuOpen,
                                    onDismissRequest = { folderMenuOpen = false }
                                ) {
                                    folders.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                onFolderChange(option)
                                                folderMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (useGitDev && folders.isEmpty()) {
                        Text(
                            "No folder found under $packagePath.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    OutlinedTextField(
                        value = message,
                        onValueChange = onMessageChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Description") },
                        placeholder = { Text("RexGit Screen Error") },
                        maxLines = 3,
                        singleLine = false
                    )

                    if (useGitDev) Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = bumpVersion,
                                role = Role.Switch,
                                onValueChange = onBumpChange
                            )
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Bump version", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Commit prefix uses the new version, e.g. [26.57] Fix: rexgit: ...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.width(12.dp))
                        Switch(checked = bumpVersion, onCheckedChange = null)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onPush,
                enabled = canCommit
            ) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Push")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
