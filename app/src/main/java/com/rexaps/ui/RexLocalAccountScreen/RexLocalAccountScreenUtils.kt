// RexLocalAccountScreen.kt
package com.rexaps.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.File
import kotlin.math.max

internal fun hasStorageAccess(ctx: Context): Boolean =
    if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
    else ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

internal fun loadStories(): List<RexStory> {
    val now = System.currentTimeMillis()
    val files = RexPaths.story.listFiles() ?: return emptyList()
    return files.mapNotNull { f ->
        val t = f.nameWithoutExtension.toLongOrNull() ?: return@mapNotNull null
        if (now - t > DAY_MS) { f.delete(); return@mapNotNull null }
        when (f.extension.lowercase()) {
            "mp4" -> RexStory(f, t, true, "", 0)
            "json" -> try {
                val j = JSONObject(f.readText())
                RexStory(f, t, false, j.optString("text"), j.optInt("gradient"))
            } catch (e: Exception) { null }
            else -> null
        }
    }.sortedBy { it.time }
}

internal fun loadVideos(): List<File> =
    RexPaths.video.listFiles()
        ?.filter { it.extension.lowercase() in setOf("mp4", "png", "jpg", "jpeg") }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()

internal fun decodeSampled(ctx: Context, uri: Uri, maxDim: Int = 2400): Bitmap? = try {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
    var s = 1
    while (max(o.outWidth, o.outHeight) / s > maxDim) s *= 2
    val raw = ctx.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = s })
    }
    raw?.let { applyExif(ctx, uri, it) }
} catch (e: Exception) { null }
