package com.rexaps.rexmusic.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.os.Build
import android.os.PowerManager
import android.widget.RemoteViews
import com.rexaps.R
import com.rexaps.rexmusic.LoadPhase
import com.rexaps.rexmusic.RexMusicUiState
import com.rexaps.rexmusic.isPlayerBusy
import kotlin.math.abs
import kotlin.math.min

internal fun RexMusicWidget.Companion.__splitImpl_render(
            context: Context, state: RexMusicUiState, art: WidgetArt.Art?,
            lyr: RexMusicWidget.Lyr, wide: Boolean
        ): RemoteViews {
            val views = RemoteViews(
                context.packageName,
                if (wide) R.layout.widget_rexmusic_player_wide else R.layout.widget_rexmusic_player
            )
            val track = state.nowPlaying
            val busy = track != null && state.phase.isPlayerBusy
            val accent = art?.accent ?: DEFAULT_ACCENT
            val title = track?.title?.takeIf { it.isNotBlank() } ?: "RexMusic"
            val artist = when {
                track == null -> "Pilih lagu untuk mulai"
                track.artist.isBlank() -> "Artis tidak diketahui"
                else -> track.artist
            }
            val source = sourceLabel(state, wide)

            views.setTextViewText(R.id.widget_track_title, title)
            views.setTextViewText(R.id.widget_track_artist, artist)
            views.setTextViewText(R.id.widget_source, source.first)
            views.setTextColor(R.id.widget_source, source.second)

            views.setTextViewText(R.id.widget_lyrics, lyr.cur.ifBlank { "♪" })
            views.setTextColor(R.id.widget_lyrics, accent)
            if (wide) views.setTextViewText(R.id.widget_lyrics_next, lyr.next)

            if (track != null && art != null) {
                views.setImageViewBitmap(R.id.widget_cover, art.cover)
                views.setImageViewBitmap(R.id.widget_bg, if (wide) art.bgWide else art.bgCompact)
            } else {
                views.setImageViewResource(R.id.widget_cover, R.drawable.rexw_cover_placeholder)
                views.setImageViewResource(R.id.widget_bg, R.drawable.rexw_bg)
            }

            views.setTextViewText(R.id.widget_time_current, formatTime(state.positionMs))
            views.setTextViewText(R.id.widget_time_total, formatTime(state.durationMs))

            views.setImageViewResource(
                R.id.widget_play_pause,
                if (state.isPlaying) R.drawable.rexw_ic_pause else R.drawable.rexw_ic_play
            )
            views.setInt(R.id.widget_play_pause, "setColorFilter", ICON_DARK)
            views.setInt(R.id.widget_play_pause, "setImageAlpha", if (busy) 140 else 255)
            views.setContentDescription(
                R.id.widget_play_pause, if (state.isPlaying) "Jeda" else "Putar"
            )

            val progress = if (state.durationMs > 0L) {
                ((state.positionMs * 1000L) / state.durationMs).toInt().coerceIn(0, 1000)
            } else 0
            views.setProgressBar(R.id.widget_progress, 1000, progress, false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                views.setColorStateList(
                    R.id.widget_progress, "setProgressTintList", ColorStateList.valueOf(accent)
                )
            }

            val open = openAppIntent(context)
            if (open != null) views.setOnClickPendingIntent(R.id.widget_root, open)
            views.setOnClickPendingIntent(R.id.widget_prev, broadcast(context, ACTION_PREV, RC_PREV))
            views.setOnClickPendingIntent(R.id.widget_next, broadcast(context, ACTION_NEXT, RC_NEXT))
            views.setOnClickPendingIntent(
                R.id.widget_play_pause,
                if (track == null && open != null) open
                else broadcast(context, ACTION_TOGGLE, RC_TOGGLE)
            )

            views.setContentDescription(
                R.id.widget_root,
                if (track == null) "RexMusic. Belum ada lagu. Ketuk untuk membuka aplikasi"
                else "$title, $artist. ${source.first.removePrefix("● ").lowercase()}. " +
                    "Ketuk untuk membuka RexMusic"
            )
            return views
        }

/** Blur murah: downscale ke 32px lalu upscale + gradient gelap→warna dominan + mask sudut bulat. */
    internal fun WidgetArt.__splitImpl_blurBackground(src: Bitmap, w: Int, h: Int, base: Int): Bitmap {
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

internal fun WidgetArt.__splitImpl_roundedSquare(src: Bitmap, size: Int, radiusFrac: Float): Bitmap {
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

internal fun RexMusicWidget.Companion.__splitImpl_sourceLabel(state: RexMusicUiState, wide: Boolean): Pair<String, Int> {
            val track = state.nowPlaying ?: return "SIAP" to COLOR_MUTED
            if (track.title.isBlank() && track.artist.isBlank()) return "SIAP" to COLOR_MUTED
            if (state.phase.isPlayerBusy) {
                val text = when (state.phase) {
                    LoadPhase.Buffering -> "BUFFERING"
                    LoadPhase.Preparing -> "MEMUAT"
                    else -> "MENYIAPKAN"
                }
                return text to COLOR_MUTED
            }
            return if (state.nowPlayingOffline) {
                (if (wide) "● OFFLINE · LOKAL" else "● OFFLINE") to COLOR_OFFLINE
            } else {
                (if (wide) "● ONLINE · STREAM" else "● ONLINE") to COLOR_ONLINE
            }
        }

internal fun RexMusicWidget.Companion.__splitImpl_shouldRender(
            context: Context, state: RexMusicUiState, art: WidgetArt.Art?,
            lyr: RexMusicWidget.Lyr, force: Boolean
        ): Boolean {
            val sig = signature(state, art, lyr)
            val pos = state.positionMs
            if (force || sig != lastSignature) {
                lastSignature = sig
                lastPositionMs = pos
                return true
            }
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null && !pm.isInteractive) return false
            if (abs(pos - lastPositionMs) < PROGRESS_STEP_MS) return false
            lastPositionMs = pos
            return true
        }

internal fun WidgetArt.__splitImpl_averageColor(src: Bitmap): Int {
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
