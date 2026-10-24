@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun EditorHeader(
    state: RexGitUiState,
    editor: RexGitEditorState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    compact: Boolean = false
) {
    val path = state.editorPath ?: return
    val scheme = MaterialTheme.colorScheme

    val repoPath = state.selectedRepository?.path ?: ""
    val name = path.substringAfterLast('/')
    val relative = path.removePrefix(repoPath).trimStart('/')
    val language = remember(path) { RexGitSyntax.detect(path) }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = if (compact) 6.dp else 12.dp,
                bottom = if (compact) 4.dp else 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderIconButton(
            icon = Icons.Default.ArrowBack,
            description = "Close editor",
            onClick = onBack
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (state.editorDirty) "Unsaved changes" else relative,
                style = MaterialTheme.typography.labelMedium,
                color = if (state.editorDirty) scheme.tertiary else scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            RexGitSyntax.label(language),
            style = MaterialTheme.typography.labelMedium,
            color = scheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        HeaderIconButton(
            icon = Icons.Default.Search,
            description = "Search",
            onClick = { editor.searchOpen.value = true }
        )

        Spacer(Modifier.width(6.dp))

        Button(
            onClick = onSave,
            enabled = state.editorDirty && !state.isSaving,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.heightIn(min = if (compact) 40.dp else 44.dp)
        ) {
            Text(if (state.isSaving) "Saving" else "Save")
        }
    }
}
