package com.rexaps.rexmanager.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.rexaps.rexmanager.RexFile
import com.rexaps.rexmanager.utils.*

@Composable
fun RexFileIcon(file: RexFile, modifier: Modifier = Modifier) {
    val (vector, tint) = when (FileTypeResolver.category(file)) {
        FileCategory.FOLDER -> Icons.Filled.Folder to Color(0xFFFFB300)
        FileCategory.IMAGE -> Icons.Filled.Image to Color(0xFF43A047)
        FileCategory.VIDEO -> Icons.Filled.VideoFile to Color(0xFFE53935)
        FileCategory.AUDIO -> Icons.Filled.AudioFile to Color(0xFF8E24AA)
        FileCategory.DOCUMENT -> Icons.Filled.Description to Color(0xFF1E88E5)
        FileCategory.ARCHIVE -> Icons.Filled.FolderZip to Color(0xFF6D4C41)
        FileCategory.CODE -> Icons.Filled.Code to Color(0xFF00897B)
        FileCategory.APK -> Icons.Filled.Android to Color(0xFF7CB342)
        FileCategory.OTHER -> Icons.Filled.InsertDriveFile to Color(0xFF757575)
    }
    Icon(vector, contentDescription = null, tint = tint, modifier = modifier)
}

fun RexFile.subtitle(): String =
    if (isDirectory) DateFormatter.format(lastModified)
    else "${FileSizeFormatter.format(size)} • ${DateFormatter.format(lastModified)}"
