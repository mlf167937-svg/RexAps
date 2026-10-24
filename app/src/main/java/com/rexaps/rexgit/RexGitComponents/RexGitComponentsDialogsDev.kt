@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Checkbox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * GitDev mode. Page 0 is the reading guide (shown before activation),
 * page 1 is the setup form with a live preview of the detected folders.
 */
@Composable
internal fun GitDevDialog(
    repo: RexGitRepository,
    config: RexGitGitDevConfig,
    onSave: (RexGitGitDevConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    var page by rememberSaveable { mutableStateOf(if (config.enabled) 1 else 0) }
    var understood by rememberSaveable { mutableStateOf(false) }
    var packagePath by rememberSaveable { mutableStateOf(config.packagePath) }
    var module by rememberSaveable { mutableStateOf(config.module) }

    var found by remember { mutableStateOf<List<String>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }

    val draft = RexGitGitDev.normalize(
        RexGitGitDevConfig(enabled = true, packagePath = packagePath, module = module)
    )

    LaunchedEffect(draft.packagePath, draft.module) {
        if (draft.packagePath.isBlank()) {
            found = emptyList()
            scanning = false
        } else {
            scanning = true
            delay(250)
            found = withContext(Dispatchers.IO) {
                RexGitGitDev.detectFolders(File(repo.path), draft)
            }
            scanning = false
        }
    }

    if (page == 0) {
        RexGitDialogFrame(
            title = "GitDev mode guide",
            onDismiss = onDismiss,
            actions = {
                TextButton(onClick = onDismiss) { Text("Cancel") }

                Button(
                    onClick = { page = 1 },
                    enabled = understood
                ) { Text("Continue") }
            }
        ) {
            Text(
                "Read this first. It explains how RexGit finds the folders used by Push.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )

            GitDevGuide()

            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = understood,
                        role = Role.Checkbox,
                        onValueChange = { understood = it }
                    )
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = understood, onCheckedChange = null)
                Spacer(Modifier.width(10.dp))
                Text("I have read the guide", style = MaterialTheme.typography.bodyLarge)
            }
        }
    } else {
        val canActivate = draft.packagePath.isNotBlank() && found.isNotEmpty() && !scanning

        RexGitDialogFrame(
            title = "GitDev setup",
            onDismiss = onDismiss,
            actions = {
                if (config.enabled) {
                    TextButton(
                        onClick = {
                            onSave(config.copy(enabled = false))
                            onDismiss()
                        }
                    ) { Text("Turn off") }
                }

                TextButton(onClick = onDismiss) { Text("Cancel") }

                Button(
                    onClick = {
                        onSave(draft)
                        onDismiss()
                    },
                    enabled = canActivate
                ) { Text(if (config.enabled) "Save" else "Activate") }
            }
        ) {
            Text(
                "Repository: ${repo.name}",
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = packagePath,
                onValueChange = { packagePath = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Package path") },
                placeholder = { Text("com/rexaps") },
                singleLine = true,
                supportingText = { Text("Folder that contains your feature folders. Dots work too.") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    autoCorrect = false,
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = module,
                onValueChange = { module = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Module folder (optional)") },
                placeholder = { Text("app") },
                singleLine = true,
                supportingText = { Text("Empty = try app/ first, then the project root.") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    autoCorrect = false,
                    imeAction = ImeAction.Done
                )
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color =
                    if (draft.packagePath.isBlank() || scanning) scheme.surfaceVariant
                    else if (found.isEmpty()) scheme.errorContainer
                    else scheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when {
                        draft.packagePath.isBlank() -> Text(
                            "Fill in the package path to scan for folders.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        scanning -> Text(
                            "Scanning...",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        found.isEmpty() -> {
                            Text(
                                "No folders found. Check the package path or module.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onErrorContainer
                            )

                            Text(
                                "Looked in:\n" + RexGitGitDev.sourceRoots(draft).joinToString("\n"),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = scheme.onErrorContainer
                            )
                        }

                        else -> {
                            Text(
                                if (found.size == 1) "1 folder found"
                                else "${found.size} folders found",
                                style = MaterialTheme.typography.titleSmall,
                                color = scheme.onPrimaryContainer
                            )

                            Text(
                                found.joinToString("  ·  "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onPrimaryContainer,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            TextButton(onClick = { page = 0 }) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Read the guide again")
            }
        }
    }
}
