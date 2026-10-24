
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.rexaps.rexgit

/** 0 = home, 1 = repository, 2 = editor. */
internal fun screenIndex(state: RexGitUiState): Int {
    return when {
        state.editorPath != null -> 2
        state.selectedRepository != null -> 1
        else -> 0
    }
}
