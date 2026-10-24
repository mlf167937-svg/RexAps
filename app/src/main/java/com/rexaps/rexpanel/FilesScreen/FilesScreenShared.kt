package com.rexaps.rexpanel

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

internal fun __splitImpl_styleOf(f: RemoteFile, fallback: Color): KindStyle = when {
    f.isDir -> KindStyle(Icons.Default.Folder, CFolder, "Folder")
    else -> when (f.kind) {
        FileKind.Image -> KindStyle(Icons.Default.Image, CImage, "Gambar")
        FileKind.Video -> KindStyle(Icons.Default.Movie, CVideo, "Video")
        FileKind.Audio -> KindStyle(Icons.Default.Audiotrack, CAudio, "Audio")
        FileKind.Text -> KindStyle(Icons.Default.Description, CText, "Teks")
        FileKind.Archive -> KindStyle(Icons.Default.Archive, CArchive, "Arsip")
        FileKind.Other -> KindStyle(Icons.Default.InsertDriveFile, fallback, "Berkas")
    }
}
