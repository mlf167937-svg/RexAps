package com.rexaps.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.LinearGradient
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.IconCompat
import com.rexaps.MainActivity
import kotlin.math.max
import kotlin.math.min

internal const val ACTION_OPEN_REX_MODULE = "com.rexaps.action.OPEN_MODULE"
internal const val EXTRA_REX_MODULE_ID = "com.rexaps.extra.MODULE_ID"

/** Builds a launcher-ready icon without writing a temporary image to external storage. */
internal fun createRexShortcutBitmap(
    context: Context,
    module: RexModule,
    accentColor: Int,
    glyph: String,
    customImage: Uri?
): Bitmap {
    val dimension = 512
    val bitmap = Bitmap.createBitmap(dimension, dimension, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fullRect = Rect(0, 0, dimension, dimension)

    if (customImage != null) {
        val source = decodeShortcutImage(context, customImage)
            ?: throw IllegalArgumentException("Gambar tidak dapat dibaca. Pilih gambar lain.")
        try {
            val side = min(source.width, source.height)
            val left = (source.width - side) / 2
            val top = (source.height - side) / 2
            val sourceRect = Rect(left, top, left + side, top + side)
            val destination = Rect(0, 0, dimension, dimension)
            val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(source, sourceRect, destination, imagePaint)
            canvas.drawRect(
                0f, 0f, dimension.toFloat(), dimension.toFloat(),
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(24, 0, 0, 0) }
            )
        } finally {
            if (!source.isRecycled) source.recycle()
        }
    } else {
        val topColor = ColorUtils.blendARGB(accentColor, android.graphics.Color.WHITE, 0.16f)
        val bottomColor = ColorUtils.blendARGB(accentColor, android.graphics.Color.BLACK, 0.28f)
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, dimension.toFloat(), dimension.toFloat(), topColor, bottomColor, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(fullRect, backgroundPaint)

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(48, 255, 255, 255)
        }
        canvas.drawCircle(410f, 86f, 170f, glowPaint)
        canvas.drawCircle(86f, 440f, 126f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(28, 0, 0, 0)
        })

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(110, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        canvas.drawRoundRect(RectF(48f, 48f, 464f, 464f), 108f, 108f, ringPaint)

        val mark = glyph.trim().ifBlank { defaultShortcutGlyph(module) }.take(3).uppercase()
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
            textSize = when (mark.length) { 1 -> 208f; 2 -> 168f; else -> 132f }
            setShadowLayer(14f, 0f, 7f, android.graphics.Color.argb(100, 0, 0, 0))
        }
        val baseline = dimension / 2f - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(mark, dimension / 2f, baseline, textPaint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
        canvas.drawCircle(402f, 400f, 18f, dotPaint)
    }

    return bitmap
}

private fun decodeShortcutImage(context: Context, uri: Uri): Bitmap? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    while (max(bounds.outWidth / sample, bounds.outHeight / sample) > 1024) sample *= 2
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}

internal fun defaultShortcutGlyph(module: RexModule): String {
    val name = module.name.removePrefix("Rex").trim()
    return when {
        name.isBlank() -> "RX"
        name.length == 1 -> name.uppercase()
        else -> name.take(2).uppercase()
    }
}

internal fun requestRexModuleShortcut(
    context: Context,
    module: RexModule,
    label: String,
    bitmap: Bitmap,
    iconVariant: String
): Boolean {
    require(module.available) { "Modul ini belum tersedia." }
    require(RexRoute.fromModuleId(module.id) != null) { "Modul belum memiliki tujuan navigasi." }
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false

    val safeLabel = label.trim().ifBlank { module.name }.take(24)
    val identity = "${safeLabel.lowercase()}|$iconVariant"
    val shortcutId = "rexaps_${module.id}_${identity.hashCode().toUInt().toString(16)}"
    val launchIntent = Intent(context, MainActivity::class.java).apply {
        action = ACTION_OPEN_REX_MODULE
        data = Uri.parse("rexaps://module/${Uri.encode(module.id)}?shortcut=${Uri.encode(shortcutId)}")
        putExtra(EXTRA_REX_MODULE_ID, module.id)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    val shortcut = ShortcutInfoCompat.Builder(context, shortcutId)
        .setShortLabel(safeLabel)
        .setLongLabel("$safeLabel · ${module.name} di RexAps".take(40))
        .setIcon(IconCompat.createWithAdaptiveBitmap(bitmap))
        .setIntent(launchIntent)
        .build()

    return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
}
