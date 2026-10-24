@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

internal fun accountSuffix(state: RexGitUiState): String {
    return if (state.accounts.size > 1) "  ·  ${state.accounts.size} accounts" else ""
}

/** Location text used by the "new file / folder" sheets and dialogs. */
internal fun locationLabel(state: RexGitUiState): String {
    val repo = state.selectedRepository ?: return ""

    val relative = state.currentDirectory
        ?.removePrefix(repo.path)
        ?.trim('/')
        ?: ""

    val segments = relative.split('/').filter { it.isNotEmpty() }

    return if (segments.isEmpty()) repo.name
    else repo.name + "/" + segments.joinToString("/")
}

internal fun fileStyle(file: RexGitFile, scheme: ColorScheme): FileStyle {
    if (file.isDirectory) return FileStyle(Icons.Default.Folder, scheme.primary)

    return when (file.name.substringAfterLast('.', "").lowercase()) {
        "kt", "kts", "java", "gradle" -> FileStyle(Icons.Default.Code, scheme.primary)
        "xml", "html", "css", "js", "ts", "json", "yml", "yaml", "toml" ->
            FileStyle(Icons.Default.Code, scheme.secondary)
        "md", "txt" -> FileStyle(Icons.Default.Description, scheme.tertiary)
        "png", "jpg", "jpeg", "webp", "gif", "svg" ->
            FileStyle(Icons.Default.Image, Color(0xFFFF8FB1))
        "properties", "pro", "gitignore" ->
            FileStyle(Icons.Default.Settings, scheme.onSurfaceVariant)
        else -> FileStyle(Icons.Default.InsertDriveFile, scheme.onSurfaceVariant)
    }
}

// ============================================================
// Utilities
// ============================================================

internal fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "%.1f MB".format(bytes / 1048576.0)
    }
}

internal fun openAllFilesAccess(context: Context) {
    val uri = Uri.parse("package:${context.packageName}")

    try {
        val intent =
            if (Build.VERSION.SDK_INT >= 30) {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
            }

        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Throwable) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Throwable) {
        }
    }
}
