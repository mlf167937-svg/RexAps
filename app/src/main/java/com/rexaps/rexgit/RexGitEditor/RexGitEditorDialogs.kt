@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Search + go-to-line dialogs. */
@Composable
internal fun __splitImpl_EditorDialogs(editor: RexGitEditorState) {
    val scheme = MaterialTheme.colorScheme

    if (editor.searchOpen.value) {
        AlertDialog(
            onDismissRequest = { editor.searchOpen.value = false },
            title = { Text("Search in file") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editor.searchQuery.value,
                        onValueChange = {
                            editor.searchQuery.value = it
                            // A new query starts before the first match so Next
                            // selects result #1 instead of unintentionally skipping it.
                            editor.searchIndex.value = -1
                        },
                        singleLine = true,
                        label = { Text("Find") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    val query = editor.searchQuery.value
                    val count = editor.searchMatches(query).size
                    Text(
                        if (query.isBlank()) "Type text to search" else "$count match${if (count == 1) "" else "es"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { editor.selectSearchMatch(-1) }, enabled = query.isNotBlank()) { Text("Previous") }
                        OutlinedButton(onClick = { editor.selectSearchMatch(1) }, enabled = query.isNotBlank()) { Text("Next") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { editor.searchOpen.value = false }) { Text("Done") } }
        )
    }

    if (editor.goToLineOpen.value) {
        AlertDialog(
            onDismissRequest = { editor.goToLineOpen.value = false },
            title = { Text("Go to line") },
            text = {
                OutlinedTextField(
                    value = editor.goToLineText.value,
                    onValueChange = { editor.goToLineText.value = it.filter(Char::isDigit) },
                    singleLine = true,
                    label = { Text("Line 1–${editor.lines}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = { TextButton(onClick = editor::goToLine) { Text("Go") } },
            dismissButton = { TextButton(onClick = { editor.goToLineOpen.value = false }) { Text("Cancel") } }
        )
    }
}
