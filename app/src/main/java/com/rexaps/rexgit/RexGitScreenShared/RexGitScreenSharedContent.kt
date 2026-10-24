@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** 0 = home, 1 = repository, 2 = editor. */
internal fun __splitImpl_screenIndex(state: RexGitUiState): Int {
    return when {
        state.editorPath != null -> 2
        state.selectedRepository != null -> 1
        else -> 0
    }
}
