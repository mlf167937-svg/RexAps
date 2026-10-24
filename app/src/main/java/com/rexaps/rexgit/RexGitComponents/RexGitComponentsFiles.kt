
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.rexaps.rexgit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal data class FileStyle(val icon: ImageVector, val color: Color)

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

@Composable
internal fun FileRow(
    file: RexGitFile,
    change: RexGitChangeKind?,
    hasChangesInside: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val style = fileStyle(file, scheme)

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = if (file.isDirectory) "Open folder" else "Open file",
                onClick = onClick
            )
            .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(style.icon, style.color)

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                file.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                if (file.isDirectory) "Folder" else formatSize(file.size),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }

        if (change != null) {
            Spacer(Modifier.width(8.dp))
            ChangeBadge(change)
        } else if (hasChangesInside) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(scheme.tertiary)
                    .semantics { contentDescription = "Contains changes" }
            )
        }

        IconButton(onClick = onMore) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Actions for ${file.name}",
                tint = scheme.onSurfaceVariant
            )
        }
    }
}


// ============================================================
// Dialog frame (scrolls, so it also works in landscape)
// ============================================================

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
