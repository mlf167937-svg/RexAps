package com.rexaps.rexmusic.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
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
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.widget.RemoteViews
import com.rexaps.R
import com.rexaps.rexmusic.LoadPhase
import com.rexaps.rexmusic.LyricLine
import com.rexaps.rexmusic.RexMusicService
import com.rexaps.rexmusic.RexMusicUiState
import com.rexaps.rexmusic.RexPlayerController
import com.rexaps.rexmusic.isPlayerBusy
import kotlin.math.abs
import kotlin.math.min

class RexMusicWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val app = context.applicationContext
        RexPlayerController.init(app)
        if (RexPlayerController.state.value.nowPlaying != null) RexMusicService.start(app)
        updateIds(app, manager, ids, RexPlayerController.state.value)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, manager, appWidgetId, newOptions)
        val app = context.applicationContext
        RexPlayerController.init(app)
        updateIds(app, manager, intArrayOf(appWidgetId), RexPlayerController.state.value)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action !in CONTROL_ACTIONS) return
        val app = context.applicationContext
        RexPlayerController.init(app)
        when (action) {
            ACTION_TOGGLE -> RexPlayerController.togglePlay()
            ACTION_NEXT -> RexPlayerController.next()
            ACTION_PREV -> RexPlayerController.prev(force = true)
            ACTION_OPEN -> openApp(app)
            ACTION_REFRESH -> Unit
        }
        updateAll(app, RexPlayerController.state.value, force = true)
    }

    private data class Lyr(val cur: String, val next: String)

    companion object {
        const val ACTION_TOGGLE = "com.rexaps.rexmusic.widget.TOGGLE"
        const val ACTION_NEXT = "com.rexaps.rexmusic.widget.NEXT"
        const val ACTION_PREV = "com.rexaps.rexmusic.widget.PREV"
        const val ACTION_OPEN = "com.rexaps.rexmusic.widget.OPEN"
        const val ACTION_REFRESH = "com.rexaps.rexmusic.widget.REFRESH"

        private val CONTROL_ACTIONS =
            setOf(ACTION_TOGGLE, ACTION_NEXT, ACTION_PREV, ACTION_OPEN, ACTION_REFRESH)

        private const val WIDE_MIN_DP = 372
        private const val PROGRESS_STEP_MS = 1_000L

        private const val DEFAULT_ACCENT = 0xFFA99BFF.toInt()
        private val COLOR_ONLINE = Color.rgb(120, 200, 255)
        private val COLOR_OFFLINE = Color.rgb(110, 235, 180)
        private val COLOR_MUTED = Color.rgb(200, 200, 215)
        private const val ICON_DARK = 0xFF121212.toInt()

        private const val RC_OPEN = 10
        private const val RC_PREV = 11
        private const val RC_TOGGLE = 12
        private const val RC_NEXT = 13

        private var lastSignature = ""
        private var lastPositionMs = -1L

        fun updateAll(
            context: Context,
            state: RexMusicUiState,
            cover: Bitmap? = null,
            coverUrl: String? = null,
            force: Boolean = false
        ) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, RexMusicWidget::class.java))
            if (ids.isEmpty()) return

            if (cover != null) WidgetArt.put(coverUrl ?: state.nowPlaying?.cover.orEmpty(), cover)
            val art = WidgetArt.forTrack(state.nowPlaying?.cover)
            val lyr = lyricLines(state)
            if (!shouldRender(app, state, art, lyr, force)) return
            renderInto(app, manager, ids, state, art, lyr)
        }

        private fun updateIds(
            context: Context, manager: AppWidgetManager, ids: IntArray, state: RexMusicUiState
        ) {
            val art = WidgetArt.forTrack(state.nowPlaying?.cover)
            val lyr = lyricLines(state)
            lastSignature = signature(state, art, lyr)
            lastPositionMs = state.positionMs
            renderInto(context, manager, ids, state, art, lyr)
        }

        private fun renderInto(
            context: Context, manager: AppWidgetManager, ids: IntArray,
            state: RexMusicUiState, art: WidgetArt.Art?, lyr: Lyr
        ) {
            val built = HashMap<Boolean, RemoteViews>(2)
            for (id in ids) {
                val wide = isWide(context, manager, id)
                val views = built.getOrPut(wide) { render(context, state, art, lyr, wide) }
                runCatching { manager.updateAppWidget(id, views) }
            }
        }

        // ---------- throttle ----------

        private fun signature(state: RexMusicUiState, art: WidgetArt.Art?, lyr: Lyr): String {
            val t = state.nowPlaying
            return listOf(
                t?.id.orEmpty(), t?.title.orEmpty(), t?.artist.orEmpty(),
                (art != null).toString(), state.isPlaying.toString(),
                state.nowPlayingOffline.toString(), state.phase.name,
                (state.durationMs / 1000L).toString(),
                lyr.cur, lyr.next
            ).joinToString("|")
        }

        private fun shouldRender(
            context: Context, state: RexMusicUiState, art: WidgetArt.Art?,
            lyr: Lyr, force: Boolean
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

        private fun isWide(context: Context, manager: AppWidgetManager, id: Int): Boolean {
            val options = manager.getAppWidgetOptions(id) ?: return false
            val landscape =
                context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val key = if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
            else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
            return options.getInt(key, 0) >= WIDE_MIN_DP
        }

        // ---------- render ----------

        private fun render(
            context: Context, state: RexMusicUiState, art: WidgetArt.Art?,
            lyr: Lyr, wide: Boolean
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

        private fun sourceLabel(state: RexMusicUiState, wide: Boolean): Pair<String, Int> {
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

        private fun lyricLines(state: RexMusicUiState): Lyr {
            if (state.nowPlaying == null) return Lyr("", "")
            val lyrics = state.lyrics
            return when {
                state.lyricsLoading -> Lyr("Memuat lirik…", "")
                lyrics.hasSynced -> {
                    val i = activeIndex(lyrics.synced, state.positionMs)
                    val cur = if (i >= 0) lyrics.synced[i].text.trim() else ""
                    val nxt = lyrics.synced.getOrNull(i + 1)?.text?.trim().orEmpty()
                    Lyr(if (cur.isBlank()) "♪" else "♪  $cur", nxt)
                }
                !lyrics.isEmpty -> Lyr("Lirik tanpa sinkronisasi · buka aplikasi", "")
                else -> Lyr("Lirik tidak tersedia", "")
            }
        }

        private fun activeIndex(list: List<LyricLine>, positionMs: Long): Int {
            var lo = 0
            var hi = list.lastIndex
            var result = -1
            while (lo <= hi) {
                val mid = (lo + hi) ushr 1
                if (list[mid].timeMs <= positionMs) { result = mid; lo = mid + 1 } else hi = mid - 1
            }
            return result
        }

        private fun formatTime(ms: Long): String {
            if (ms <= 0L) return "00:00"
            val total = ms / 1000L
            return "%02d:%02d".format(total / 60, total % 60)
        }

        // ---------- intents ----------

        private fun broadcast(context: Context, action: String, request: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context, request,
                Intent(context, RexMusicWidget::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        private fun openAppIntent(context: Context): PendingIntent? {
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: return null
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return PendingIntent.getActivity(
                context, RC_OPEN, launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun openApp(context: Context) {
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: return
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            runCatching { context.startActivity(launch) }
        }
    }
}

/**
 * Artwork widget (cache 1 lagu): cover bulat, background blur premium (compact & wide), warna aksen.
 */
private object WidgetArt {
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