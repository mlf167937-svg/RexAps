@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** Location text used by the "new file / folder" sheets and dialogs. */
internal fun __splitImpl_locationLabel(state: RexGitUiState): String {
    val repo = state.selectedRepository ?: return ""

    val relative = state.currentDirectory
        ?.removePrefix(repo.path)
        ?.trim('/')
        ?: ""

    val segments = relative.split('/').filter { it.isNotEmpty() }

    return if (segments.isEmpty()) repo.name
    else repo.name + "/" + segments.joinToString("/")
}

internal fun __splitImpl_formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "%.1f MB".format(bytes / 1048576.0)
    }
}
