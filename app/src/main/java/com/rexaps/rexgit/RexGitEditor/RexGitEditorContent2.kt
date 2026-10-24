@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_EditorToolButton(
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

@Composable
internal fun __splitImpl_EditorStatusText(
    editor: RexGitEditorState,
    modifier: Modifier = Modifier
) {
    Text(
        "${editor.lines} lines  •  ${editor.textField.text.length} characters",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}
