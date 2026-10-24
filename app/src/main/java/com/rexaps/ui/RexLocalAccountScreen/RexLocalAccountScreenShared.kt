package com.rexaps.ui

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import java.io.File
import java.io.InputStream

/* ═════════════════════ Konstanta & Model ═════════════════════ */

internal const val DAY_MS = 24L * 60 * 60 * 1000

internal const val MAX_CLIP_MS = 4000L

internal const val BANNER_ASPECT = 16f / 9f

internal object RexPaths {
    val base get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RexAps/.config")
    val account get() = File(base, "account")
    val video get() = File(account, "video")
    val story get() = File(base, "story")
    val config get() = File(base, "account.json")
}

internal data class RexLocalAccount(
    val name: String = "Rex User",
    val username: String = "@rexuser",
    val bio: String = "Halo! Aku menggunakan RexAps.",
    val bannerPath: String = "",
    val bannerVideo: String = "",
    val bannerScale: Float = 1f,
    val bannerX: Float = 0f,
    val bannerY: Float = 0f,
    val picturePath: String = "",
    val pictureVideo: String = "",
    val pictureScale: Float = 1f,
    val pictureX: Float = 0f,
    val pictureY: Float = 0f
)

internal data class RexStory(
    val file: File,
    val time: Long,
    val isVideo: Boolean,
    val text: String,
    val gradient: Int
)

internal enum class CropTarget { AVATAR, BANNER }

internal class VSrc(val uri: Uri?, val file: File?) {
    fun bind(r: MediaMetadataRetriever, c: Context) {
        if (uri != null) r.setDataSource(c, uri) else r.setDataSource(file!!.absolutePath)
    }

    fun bind(e: MediaExtractor, c: Context) {
        if (uri != null) e.setDataSource(c, uri, null) else e.setDataSource(file!!.absolutePath)
    }

    fun open(c: Context): InputStream? =
        if (uri != null) c.contentResolver.openInputStream(uri) else file?.inputStream()
}

internal class CropReq(
    val target: CropTarget,
    val bitmap: Bitmap,
    val video: VSrc?,
    val durationMs: Long,
    val live: Boolean
)

internal val storyGradients = listOf(
    listOf(Color(0xFFFF512F), Color(0xFFDD2476)),
    listOf(Color(0xFF4776E6), Color(0xFF8E54E9)),
    listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
    listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
    listOf(Color(0xFF232526), Color(0xFF414345)),
    listOf(Color(0xFFEE0979), Color(0xFFFF6A00))
)
