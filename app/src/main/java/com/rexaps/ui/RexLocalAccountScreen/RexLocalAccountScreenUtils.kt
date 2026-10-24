package com.rexaps.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
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
import java.io.FileOutputStream
import kotlin.math.max

/* ═════════════════════ File & Media Helpers ═════════════════════ */

internal fun hasStorageAccess(ctx: Context): Boolean =
    if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
    else ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

internal fun readAccount(): RexLocalAccount? = try {
    val f = RexPaths.config
    if (!f.exists()) null else JSONObject(f.readText()).let { j ->
        var bp = j.optString("banner", "")
        var bv = j.optString("bannerVideo", "")
        if (bp.endsWith(".mp4")) { bv = bp; bp = "" }
        RexLocalAccount(
            j.optString("name", "Rex User"), j.optString("username", "@rexuser"), j.optString("bio", ""),
            bp, bv,
            j.optDouble("bannerScale", 1.0).toFloat(), j.optDouble("bannerX", 0.0).toFloat(), j.optDouble("bannerY", 0.0).toFloat(),
            j.optString("profilePicture", ""), j.optString("profileVideo", ""),
            j.optDouble("pictureScale", 1.0).toFloat(), j.optDouble("pictureX", 0.0).toFloat(), j.optDouble("pictureY", 0.0).toFloat()
        )
    }
} catch (e: Exception) { null }

internal fun writeAccount(a: RexLocalAccount) {
    RexPaths.config.parentFile?.mkdirs()
    val j = JSONObject()
        .put("name", a.name).put("username", a.username).put("bio", a.bio)
        .put("banner", a.bannerPath).put("bannerVideo", a.bannerVideo)
        .put("bannerScale", a.bannerScale.toDouble()).put("bannerX", a.bannerX.toDouble()).put("bannerY", a.bannerY.toDouble())
        .put("profilePicture", a.picturePath).put("profileVideo", a.pictureVideo)
        .put("pictureScale", a.pictureScale.toDouble()).put("pictureX", a.pictureX.toDouble()).put("pictureY", a.pictureY.toDouble())
        .put("updatedAt", System.currentTimeMillis())
    RexPaths.config.writeText(j.toString(2))
}

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

internal fun decodeFileSampled(path: String, maxDim: Int): Bitmap? = try {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, o)
    var s = 1
    while (max(o.outWidth, o.outHeight) / s > maxDim) s *= 2
    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = s })
} catch (e: Exception) { null }

internal fun applyExif(ctx: Context, uri: Uri, b: Bitmap): Bitmap {
    val o = try {
        ctx.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    } catch (e: Exception) { null } ?: ExifInterface.ORIENTATION_NORMAL

    val m = android.graphics.Matrix()
    when (o) {
        ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
        else -> return b
    }
    return try { Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true) } catch (e: Exception) { b }
}

internal fun rotate90(b: Bitmap): Bitmap {
    val m = android.graphics.Matrix().apply { postRotate(90f) }
    return Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
}

private fun downscale(b: Bitmap, maxDim: Int): Bitmap {
    val m = max(b.width, b.height)
    if (m <= maxDim) return b
    val k = maxDim.toFloat() / m
    return Bitmap.createScaledBitmap(b, (b.width * k).toInt().coerceAtLeast(1), (b.height * k).toInt().coerceAtLeast(1), true)
}

internal fun videoFrameAt(ctx: Context, src: VSrc, ms: Long): Bitmap? {
    val r = MediaMetadataRetriever()
    return try {
        src.bind(r, ctx)
        val f = r.getFrameAtTime(ms * 1000, MediaMetadataRetriever.OPTION_PREVIOUS_SYNC) ?: r.getFrameAtTime(0)
        f?.let { downscale(it, 1600) }
    } catch (e: Exception) { null } finally { try { r.release() } catch (_: Exception) {} }
}

internal fun videoDurationOf(ctx: Context, src: VSrc): Long {
    val r = MediaMetadataRetriever()
    return try {
        src.bind(r, ctx)
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } catch (e: Exception) { 0L } finally { try { r.release() } catch (_: Exception) {} }
}

internal fun extractMotionVideo(ctx: Context, uri: Uri, out: File): Boolean = try {
    val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
    var start = -1
    if (bytes.size > 4096 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) {
        var i = 2048
        while (i < bytes.size - 16) {
            if (bytes[i] == 'f'.code.toByte() && bytes[i + 1] == 't'.code.toByte() &&
                bytes[i + 2] == 'y'.code.toByte() && bytes[i + 3] == 'p'.code.toByte()
            ) {
                val boxSize = ((bytes[i - 4].toInt() and 0xFF) shl 24) or
                    ((bytes[i - 3].toInt() and 0xFF) shl 16) or
                    ((bytes[i - 2].toInt() and 0xFF) shl 8) or
                    (bytes[i - 1].toInt() and 0xFF)
                if (boxSize in 16..64) { start = i - 4; break }
            }
            i++
        }
    }
    if (start > 0 && bytes.size - start > 2048) {
        FileOutputStream(out).use { it.write(bytes, start, bytes.size - start) }
        true
    } else false
} catch (e: Exception) { false }
