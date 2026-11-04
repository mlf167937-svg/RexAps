package com.rexaps.rexmusic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import kotlin.math.min

/** Small LAN-safe profile images. Original account files are never modified. */
internal data class RexSyncProfileAssets(
    val pictureBase64: String = "",
    val bannerBase64: String = ""
)

internal fun createRexSyncProfileAssets(
    context: Context,
    profile: RexLocalProfileSnapshot
): RexSyncProfileAssets {
    return RexSyncProfileAssets(
        pictureBase64 = encodeProfileImage(context, profile.picturePath, 420, 420, 180_000),
        bannerBase64 = encodeProfileImage(context, profile.bannerPath, 960, 420, 260_000)
    )
}

private fun openProfileInput(context: Context, path: String): InputStream? {
    return when {
        path.startsWith("content://") -> context.contentResolver.openInputStream(Uri.parse(path))
        path.startsWith("file://") -> {
            val filePath = Uri.parse(path).path ?: return null
            File(filePath).takeIf { it.isFile }?.inputStream()
        }
        else -> File(path).takeIf { it.isFile }?.inputStream()
    }
}

private fun encodeProfileImage(
    context: Context,
    path: String,
    maxWidth: Int,
    maxHeight: Int,
    maxBytes: Int
): String {
    if (path.isBlank() || path.startsWith("http://") || path.startsWith("https://")) return ""
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching { openProfileInput(context, path)?.use { BitmapFactory.decodeStream(it, null, bounds) } }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return ""

    var sample = 1
    while (bounds.outWidth / sample > maxWidth * 2 || bounds.outHeight / sample > maxHeight * 2) {
        sample *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
    val source = runCatching {
        openProfileInput(context, path)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull() ?: return ""

    return try {
        val scale = min(1f, min(maxWidth.toFloat() / source.width.coerceAtLeast(1), maxHeight.toFloat() / source.height.coerceAtLeast(1)))
        val width = (source.width * scale).toInt().coerceAtLeast(1)
        val height = (source.height * scale).toInt().coerceAtLeast(1)
        val resized = if (width != source.width || height != source.height) {
            Bitmap.createScaledBitmap(source, width, height, true)
        } else source

        var encoded = ByteArray(0)
        for (quality in listOf(84, 76, 68, 58, 48)) {
            val output = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, quality, output)
            encoded = output.toByteArray()
            if (encoded.size <= maxBytes) break
        }
        if (resized !== source) resized.recycle()
        if (encoded.size > maxBytes || encoded.isEmpty()) ""
        else Base64.encodeToString(encoded, Base64.NO_WRAP)
    } catch (_: Exception) {
        ""
    } finally {
        runCatching { source.recycle() }
    }
}

internal fun decodeRexSyncProfileImage(encoded: String): Bitmap? {
    if (encoded.isBlank()) return null
    return runCatching {
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}
