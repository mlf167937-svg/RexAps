// RexLocalAccountScreen.kt
package com.rexaps.ui

import android.content.Context
import android.graphics.Bitmap
import android.media.ExifInterface
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
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
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.math.max

internal const val DAY_MS = 24L * 60 * 60 * 1000

internal const val MAX_CLIP_MS = 4000L

internal object RexPaths {
    val base get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RexAps/.config")
    val account get() = File(base, "account")
    val video get() = File(account, "video")
    val story get() = File(base, "story")
    val config get() = File(base, "account.json")
}

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

internal fun downscale(b: Bitmap, maxDim: Int): Bitmap {
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

internal fun trimVideo(ctx: Context, src: VSrc, startMs: Long, out: File): Boolean {
    val dur = videoDurationOf(ctx, src)
    if (dur in 1..(MAX_CLIP_MS + 300)) {
        return try {
            src.open(ctx)?.use { i -> FileOutputStream(out).use { i.copyTo(it) } }
            true
        } catch (e: Exception) { false }
    }
    val ex = MediaExtractor()
    var mux: MediaMuxer? = null
    return try {
        src.bind(ex, ctx)
        var vt = -1
        for (i in 0 until ex.trackCount) {
            if (ex.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) { vt = i; break }
        }
        if (vt < 0) return false
        ex.selectTrack(vt)
        val fmt = ex.getTrackFormat(vt)
        val maxIn = if (fmt.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) fmt.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else 0
        val buf = ByteBuffer.allocate(max(maxIn, 8 * 1024 * 1024))

        var rot = 0
        val r = MediaMetadataRetriever()
        try {
            src.bind(r, ctx)
            rot = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        } catch (_: Exception) {
        } finally { try { r.release() } catch (_: Exception) {} }

        out.delete()
        mux = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val mt = mux.addTrack(fmt)
        mux.setOrientationHint(rot)
        mux.start()
        ex.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        val info = MediaCodec.BufferInfo()
        var first = -1L
        while (true) {
            buf.clear()
            val size = ex.readSampleData(buf, 0)
            if (size < 0) break
            val t = ex.sampleTime
            if (first < 0) first = t
            if (t - first > MAX_CLIP_MS * 1000) break
            val flags = if (ex.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
            info.set(0, size, t - first, flags)
            mux.writeSampleData(mt, buf, info)
            ex.advance()
        }
        mux.stop()
        true
    } catch (e: Exception) { false }
    finally {
        try { mux?.release() } catch (_: Exception) {}
        try { ex.release() } catch (_: Exception) {}
    }
}

internal fun buildCropReq(ctx: Context, uri: Uri, target: CropTarget): CropReq? {
    val mime = ctx.contentResolver.getType(uri) ?: ""
    if (mime.startsWith("video")) {
        val src = VSrc(uri, null)
        val dur = videoDurationOf(ctx, src)
        val frame = videoFrameAt(ctx, src, 0) ?: return null
        return CropReq(target, frame, src, dur, false)
    }
    val photo = decodeSampled(ctx, uri) ?: return null
    if (mime == "image/jpeg") {
        val f = File(ctx.cacheDir, "live_${System.currentTimeMillis()}.mp4")
        if (extractMotionVideo(ctx, uri, f)) {
            val src = VSrc(null, f)
            val dur = videoDurationOf(ctx, src)
            val frame = videoFrameAt(ctx, src, 0)
            if (dur > 0 && frame != null) {
                val sameAspect = Math.abs(
                    photo.width.toFloat() / photo.height - frame.width.toFloat() / frame.height
                ) < 0.02f
                return CropReq(target, if (sameAspect) photo else frame, src, dur, true)
            }
        }
        f.delete()
    }
    return CropReq(target, photo, null, 0, false)
}

internal fun renderCrop(src: Bitmap, aspect: Float, outW: Int, s: Float, ox: Float, oy: Float): Bitmap {
    val outH = (outW / aspect).toInt()
    val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val base = max(outW.toFloat() / src.width, outH.toFloat() / src.height) * s
    val m = android.graphics.Matrix().apply {
        postTranslate(-src.width / 2f, -src.height / 2f)
        postScale(base, base)
        postTranslate(outW / 2f + ox * outW, outH / 2f + oy * outH)
    }
    android.graphics.Canvas(out).drawBitmap(
        src, m,
        android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG or android.graphics.Paint.DITHER_FLAG)
    )
    return out
}

internal fun secs(ms: Long) = String.format("%.1fs", ms / 1000f)

/* ═════════════════════ UI Utilities ═════════════════════ */
