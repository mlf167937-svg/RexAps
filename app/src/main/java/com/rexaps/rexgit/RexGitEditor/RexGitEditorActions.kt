@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun rememberEditorActions(editor: RexGitEditorState): List<EditorAction> {
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

@Composable
internal fun EditorToolButton(
    action: EditorAction,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = action.onClick,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = modifier.heightIn(min = 40.dp)
    ) {
        if (action.icon != null) {
            Icon(action.icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(action.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
