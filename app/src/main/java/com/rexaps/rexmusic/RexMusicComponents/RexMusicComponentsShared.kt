package com.rexaps.rexmusic

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap

//  Models & actions 

/** Target menu opsi lagu. onPlay = null berarti menu "Putar" disembunyikan. */
class MenuTarget(val track: RexTrack, val onPlay: (() -> Unit)?)

class QuickTile(
    val id: String,
    val title: String,
    val cover: String,
    val special: Boolean = false,
    val onClick: () -> Unit
)

class RexActions(
    val playSearch: (Int) -> Unit,
    val playHistory: (Int) -> Unit,
    val playMain: (Int) -> Unit,
    val playOffline: (RexTrack) -> Unit,
    val playOfflineAll: (Boolean) -> Unit,
    val playMix: () -> Unit,
    val playQueue: (Int) -> Unit,
    val addQueue: (RexTrack) -> Unit,
    val removeQueue: (Int) -> Unit,
    val moveQueueTop: (Int) -> Unit,
    val clearQueue: () -> Unit,
    val download: (RexTrack) -> Unit,
    val cancelDownload: (RexTrack) -> Unit,
    val delete: (RexTrack) -> Unit,
    val deleteAll: () -> Unit,
    val clearHistory: () -> Unit,
    val requestAccess: () -> Unit,
    val setOffline: (Boolean) -> Unit,
    val toggleAutoplay: () -> Unit,
    val searchArtist: (String) -> Unit,
    val goLibrary: () -> Unit,
    val goPlaylists: () -> Unit,
    val openMenu: (MenuTarget) -> Unit,
    val openPlaylistPicker: (RexTrack) -> Unit,
    val playPlaylist: (String) -> Unit,
    val playPlaylistTrack: (String, String) -> Unit,
    val createPlaylist: (String) -> Unit,
    val deletePlaylist: (String) -> Unit,
    val dismissError: () -> Unit
)

enum class CoverOverlay { None, Loading, Playing }

fun overlayFor(active: Boolean, playing: Boolean, loading: Boolean): CoverOverlay = when {
    active && loading -> CoverOverlay.Loading
    active && playing -> CoverOverlay.Playing
    else -> CoverOverlay.None
}

//  Helpers 

/** Rata-rata warna dari bitmap 16x16 (murah, tanpa Palette). */
fun Drawable.averageColor(): Color? = runCatching {
    val bmp = toBitmap(16, 16, Bitmap.Config.ARGB_8888)
    val px = IntArray(256)
    bmp.getPixels(px, 0, 16, 0, 0, 16, 16)
    var r = 0L
    var g = 0L
    var b = 0L
    px.forEach {
        r += ((it shr 16) and 0xFF)
        g += ((it shr 8) and 0xFF)
        b += (it and 0xFF)
    }
    Color((r / 256).toInt(), (g / 256).toInt(), (b / 256).toInt())
}.getOrNull()

private val TilePalette = listOf(
    Color(0xFF7E57C2), Color(0xFF1E88E5), Color(0xFFE5533D), Color(0xFF00897B),
    Color(0xFFD81B60), Color(0xFF3949AB), Color(0xFF43A047), Color(0xFFF57C00)
)

fun rexTileColor(name: String): Color =
    TilePalette[(name.hashCode() and 0x7fffffff) % TilePalette.size]

fun formatBytes(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.2f GB".format(bytes / 1073741824.0)
    bytes >= 1L shl 20 -> "%.1f MB".format(bytes / 1048576.0)
    bytes >= 1L shl 10 -> "%d KB".format(bytes shr 10)
    else -> "$bytes B"
}

fun greetingForHour(hour: Int): String = when (hour) {
    in 4..10 -> "Selamat pagi"
    in 11..14 -> "Selamat siang"
    in 15..17 -> "Selamat sore"
    else -> "Selamat malam"
}
