
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.rexaps.rexgit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun CreateSheet(
    location: String,
    onDismiss: () -> Unit,
    onPick: (CreateKind) -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surface
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Text("Create new", style = MaterialTheme.typography.titleLarge)

            Text(
                "in $location",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            SheetAction(
                icon = Icons.Default.NoteAdd,
                title = "New file",
                subtitle = "Create an empty file and open it in the editor",
                onClick = { onPick(CreateKind.FILE) }
            )

            SheetAction(
                icon = Icons.Default.CreateNewFolder,
                title = "New folder",
                subtitle = "Create an empty folder here",
                onClick = { onPick(CreateKind.FOLDER) }
            )
        }
    }
}

@Composable
internal fun ItemActionsSheet(
    file: RexGitFile,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val style = fileStyle(file, scheme)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surface
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconTile(style.icon, style.color)

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        file.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        if (file.isDirectory) "Folder" else formatSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            SheetAction(
                icon = if (file.isDirectory) Icons.Default.FolderOpen else Icons.Default.Description,
                title = if (file.isDirectory) "Open folder" else "Edit file",
                subtitle = null,
                onClick = onOpen
            )

            SheetAction(
                icon = Icons.Default.Delete,
                title = if (file.isDirectory) "Delete folder" else "Delete file",
                subtitle = if (file.isDirectory) "Removes the folder and everything inside it"
                else "Permanently removes this file",
                destructive = true,
                onClick = onDelete
            )
        }
    }
}

@Composable
internal fun SheetAction(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val tint = if (destructive) scheme.error else scheme.primary

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon, tint)

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (destructive) scheme.error else scheme.onSurface
            )

            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}
