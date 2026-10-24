@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxHeight

// ============================================================
// Editor (landscape): text on the left, tools on the right
// ============================================================

@Composable
internal fun EditorScreenLandscape(
    state: RexGitUiState,
    editor: RexGitEditorState,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val actions = rememberEditorActions(editor)

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .landscapeSideInsets()
            .imePadding()
    ) {
        EditorHeader(
            state = state,
            editor = editor,
            onBack = onBack,
            onSave = onSave,
            compact = true
        )

        ProgressSlot(state.isSaving)

        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 12.dp)
            ) {
                EditorTextArea(
                    state = state,
                    editor = editor,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                EditorStatusText(
                    editor = editor,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Column(
                Modifier
                    .width(LandscapeEditorPanelWidth)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Tools",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )

                actions.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { action ->
                            EditorToolButton(action, Modifier.weight(1f))
                        }

                        if (pair.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }

                Text(
                    "Text size",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )

                EditorFontControls(
                    editor = editor,
                    modifier = Modifier.fillMaxWidth(),
                    stacked = true
                )

                Text(
                    "Symbols",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )

                EditorKeys.chunked(4).forEach { rowKeys ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowKeys.forEach { (label, snippet) ->
                            EditorKeyChip(
                                label = label,
                                onClick = { editor.insert(snippet) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        repeat(4 - rowKeys.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
