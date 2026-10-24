@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

@Composable
internal fun __splitImpl_rememberEditorActions(editor: RexGitEditorState): List<EditorAction> {
    val clipboard = LocalClipboardManager.current

    return remember(editor, clipboard) {
        listOf(
            EditorAction("Search", Icons.Default.Search) {
                editor.searchOpen.value = true
            },
            EditorAction("Go to line", null) {
                editor.goToLineText.value = "${editor.currentLine()}"
                editor.goToLineOpen.value = true
            },
            EditorAction("Copy all", Icons.Default.ContentCopy) {
                clipboard.setText(AnnotatedString(editor.textField.text))
                editor.message("All text copied")
            },
            EditorAction("Paste", Icons.Default.ContentPaste) {
                val text = clipboard.getText()?.text.orEmpty()
                if (text.isNotEmpty()) editor.insert(text) else editor.message("Clipboard is empty")
            },
            EditorAction("Select all", Icons.Default.SelectAll) {
                editor.selectAll()
            }
        )
    }
}
