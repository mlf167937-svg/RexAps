package com.rexaps.ui

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.math.max

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

internal fun Modifier.pressable(onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(
        if (pressed) .93f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "press"
    )
    this.graphicsLayer { scaleX = sc; scaleY = sc }
        .clickable(interactionSource = src, indication = null, onClick = onClick)
}
