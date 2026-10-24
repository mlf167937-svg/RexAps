package com.rexaps.rexpanel

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val CFolder = Color(0xFFF9E2AF)

internal val CImage = Color(0xFF89B4FA)

internal val CVideo = Color(0xFFF38BA8)

internal val CAudio = Color(0xFFCBA6F7)

internal val CText = Color(0xFF94E2D5)

internal val CArchive = Color(0xFFFAB387)

internal val PreviewFg = Color(0xFFCDD6F4)

internal const val PLACE_TERMUX = "/data/data/com.termux"

internal const val PLACE_STORAGE = "/storage/emulated/0"

internal const val THUMB_MAX = 3L * 1024 * 1024       // thumbnail hanya untuk gambar <= 3 MB

internal const val AUTO_DOWNLOAD = 80L * 1024 * 1024  // video/audio > 80 MB minta konfirmasi dulu

internal val dateFmt = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("id", "ID"))

internal fun fmtDate(sec: Long): String = dateFmt.format(Date(sec * 1000))

internal fun fmtBytes(b: Long): String = when {
    b >= 1L shl 30 -> String.format("%.1f GB", b / (1L shl 30).toDouble())
    b >= 1L shl 20 -> String.format("%.1f MB", b / (1L shl 20).toDouble())
    b >= 1L shl 10 -> String.format("%.0f KB", b / 1024.0)
    else -> "$b B"
}

internal data class KindStyle(val icon: ImageVector, val color: Color, val label: String)

internal fun styleOf(f: RemoteFile, fallback: Color): KindStyle = when {
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

internal sealed interface ImgLoad {
    data class Ready(val bmp: ImageBitmap) : ImgLoad
    data class Failed(val msg: String) : ImgLoad
}
