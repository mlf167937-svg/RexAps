package com.rexaps.rexmusic.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.min

/**
 * Artwork widget (cache 1 lagu): cover bulat, background blur premium (compact & wide), warna aksen.
 */
internal object WidgetArt {
    class Art(
        val url: String,
        val cover: Bitmap,
        val bgCompact: Bitmap,
        val bgWide: Bitmap,
        val accent: Int
    )

    @Volatile
    private var current: Art? = null

    @Synchronized
    fun put(url: String, source: Bitmap) {
        if (url.isBlank() || source.isRecycled) return
        if (current?.url == url && current?.cover?.isRecycled == false) return
        runCatching {
            val soft: Bitmap = (if (source.config == Bitmap.Config.HARDWARE) {
                source.copy(Bitmap.Config.ARGB_8888, false)
            } else source) ?: return
            val avg = averageColor(soft)
            val hsv = FloatArray(3)
            Color.colorToHSV(avg, hsv)
            val sat = hsv[1].coerceIn(0.35f, 0.85f)
            val accent = Color.HSVToColor(floatArrayOf(hsv[0], (sat * 0.65f).coerceAtMost(0.7f), 1f))
            val base = Color.HSVToColor(floatArrayOf(hsv[0], sat, 0.30f))
            current = Art(
                url = url,
                cover = roundedSquare(soft, 192, 0.2f),
                bgCompact = blurBackground(soft, 640, 200, base),
                bgWide = blurBackground(soft, 800, 200, base),
                accent = accent
            )
        }
    }

    fun forTrack(url: String?): Art? =
        current?.takeIf { !url.isNullOrBlank() && it.url == url }

    private fun roundedSquare(src: Bitmap, size: Int, radiusFrac: Float): Bitmap {
        val side = min(src.width, src.height)
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val dst = RectF(0f, 0f, size.toFloat(), size.toFloat())
        val r = size * radiusFrac
        canvas.drawRoundRect(dst, r, r, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val crop = Rect(
            (src.width - side) / 2, (src.height - side) / 2,
            (src.width + side) / 2, (src.height + side) / 2
        )
        canvas.drawBitmap(src, crop, dst, paint)
        // hairline highlight
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(46, 255, 255, 255)
        }
        canvas.drawRoundRect(RectF(1f, 1f, size - 1f, size - 1f), r, r, border)
        return out
    }

    /** Blur murah: downscale ke 32px lalu upscale + gradient gelap→warna dominan + mask sudut bulat. */
    private fun blurBackground(src: Bitmap, w: Int, h: Int, base: Int): Bitmap {
        val n = 32
        val tiny = Bitmap.createScaledBitmap(src, n, n, true)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())

        val sh = (n.toFloat() * h / w).toInt().coerceAtLeast(1)
        val top = (n - sh) / 2
        c.drawBitmap(tiny, Rect(0, top, n, top + sh), rect, p)

        // peredup umum
        c.drawColor(Color.argb(95, 0, 0, 0))

        // gradient horizontal: kiri gelap pekat -> kanan transparan berwarna
        val left = Color.argb(238, Color.red(base) / 2, Color.green(base) / 2, Color.blue(base) / 2)
        val mid = Color.argb(170, Color.red(base), Color.green(base), Color.blue(base))
        val right = Color.argb(70, Color.red(base), Color.green(base), Color.blue(base))
        p.shader = LinearGradient(0f, 0f, w.toFloat(), 0f,
            intArrayOf(left, mid, right), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(rect, p)

        // vignette bawah
        p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(),
            Color.TRANSPARENT, Color.argb(90, 0, 0, 0), Shader.TileMode.CLAMP)
        c.drawRect(rect, p)
        p.shader = null

        // mask sudut bulat
        val mask = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        c.drawRoundRect(rect, 48f, 48f, mask)

        // border tipis
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.argb(30, 255, 255, 255)
        }
        c.drawRoundRect(RectF(1f, 1f, w - 1f, h - 1f), 48f, 48f, stroke)
        return out
    }

    private fun averageColor(src: Bitmap): Int {
        val small = Bitmap.createScaledBitmap(src, 8, 8, true)
        val px = IntArray(64)
        small.getPixels(px, 0, 8, 0, 0, 8, 8)
        var r = 0L; var g = 0L; var b = 0L; var n = 0
        for (p in px) {
            if ((p ushr 24) < 128) continue
            r += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF; n++
        }
        if (n == 0) return 0xFF5B47E0.toInt()
        return Color.rgb((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
    }
}
