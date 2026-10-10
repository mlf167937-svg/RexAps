package com.rexaps.rexmusic.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
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

/**
 * Widget home-screen RexMusic: compact 4x1 (default) dan extended 5x1.
 *
 * Fitur:
 * - Cover besar di sisi kanan sebagai background (dengan scrim gradient).
 * - Baris lirik kecil (1 baris) di compact dan wide.
 * - Durasi mm:ss di kiri & kanan progress bar.
 */
class RexMusicWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val app = context.applicationContext
        RexPlayerController.init(app)
        // Pastikan service hidup supaya cover di-load dan widget dapat update lanjutan.
        if (RexPlayerController.state.value.nowPlaying != null) {
            RexMusicService.start(app)
        }
        updateIds(app, manager, ids, RexPlayerController.state.value)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
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

        private const val DEFAULT_TINT = 0xFF5B47E0.toInt()

        private val COLOR_ONLINE = Color.rgb(95, 190, 255)
        private val COLOR_OFFLINE = Color.rgb(96, 230, 170)
        private val COLOR_MUTED = Color.rgb(176, 176, 196)

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

            if (cover != null) {
                WidgetArt.put(coverUrl ?: state.nowPlaying?.cover.orEmpty(), cover)
            }
            val art = WidgetArt.forTrack(state.nowPlaying?.cover)
            val lyric = lyricLine(state)
            if (!shouldRender(app, state, art, lyric, force)) return
            renderInto(app, manager, ids, state, art, lyric)
        }

        private fun updateIds(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray,
            state: RexMusicUiState
        ) {
            val art = WidgetArt.forTrack(state.nowPlaying?.cover)
            val lyric = lyricLine(state)
            lastSignature = signature(state, art, lyric)
            lastPositionMs = state.positionMs
            renderInto(context, manager, ids, state, art, lyric)
        }

        private fun renderInto(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray,
            state: RexMusicUiState,
            art: WidgetArt.Art?,
            lyric: String
        ) {
            val built = HashMap<Boolean, RemoteViews>(2)
            for (id in ids) {
                val wide = isWide(context, manager, id)
                val views = built.getOrPut(wide) { render(context, state, art, lyric, wide) }
                runCatching { manager.updateAppWidget(id, views) }
            }
        }

        // ---------- throttle ----------

        private fun signature(state: RexMusicUiState, art: WidgetArt.Art?, lyric: String): String {
            val t = state.nowPlaying
            return listOf(
                t?.id.orEmpty(), t?.title.orEmpty(), t?.artist.orEmpty(),
                (art != null).toString(), state.isPlaying.toString(),
                state.nowPlayingOffline.toString(), state.phase.name,
                (state.durationMs / 1000L).toString(),
                state.lyricsLoading.toString(),
                state.lyrics.isEmpty.toString(),
                state.lyrics.synced.size.toString(),
                lyric
            ).joinToString("|")
        }

        private fun shouldRender(
            context: Context,
            state: RexMusicUiState,
            art: WidgetArt.Art?,
            lyric: String,
            force: Boolean
        ): Boolean {
            val sig = signature(state, art, lyric)
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

        // ---------- ukuran ----------

        private fun isWide(context: Context, manager: AppWidgetManager, id: Int): Boolean {
            val options = manager.getAppWidgetOptions(id) ?: return false
            val landscape =
                context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val key = if (landscape) {
                AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
            } else {
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
            }
            return options.getInt(key, 0) >= WIDE_MIN_DP
        }

        // ---------- render ----------

        private fun render(
            context: Context,
            state: RexMusicUiState,
            art: WidgetArt.Art?,
            lyric: String,
            wide: Boolean
        ): RemoteViews {
            val views = RemoteViews(
                context.packageName,
                if (wide) R.layout.widget_rexmusic_player_wide else R.layout.widget_rexmusic_player
            )
            val track = state.nowPlaying
            val busy = track != null && state.phase.isPlayerBusy
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

            val safeLyric = lyric.ifBlank { "♪" }
            if (wide) {
                views.setTextViewText(R.id.widget_lyrics, safeLyric)
            }
            views.setTextViewText(R.id.widget_lyrics_small, safeLyric)

            if (track != null && art != null) {
                views.setImageViewBitmap(R.id.widget_cover, art.bitmap)
                views.setImageViewBitmap(R.id.widget_cover_bg, art.bitmap)
            } else {
                views.setImageViewResource(R.id.widget_cover, R.drawable.rexw_cover_placeholder)
                views.setImageViewResource(
                    R.id.widget_cover_bg,
                    R.drawable.rexw_cover_placeholder
                )
            }

            views.setInt(R.id.widget_tint, "setColorFilter", art?.tint ?: DEFAULT_TINT)

            views.setTextViewText(R.id.widget_time_current, formatTime(state.positionMs))
            views.setTextViewText(R.id.widget_time_total, formatTime(state.durationMs))

            views.setImageViewResource(
                R.id.widget_play_pause,
                if (state.isPlaying) R.drawable.rexw_ic_pause else R.drawable.rexw_ic_play
            )
            views.setInt(R.id.widget_play_pause, "setImageAlpha", if (busy) 140 else 255)
            views.setContentDescription(
                R.id.widget_play_pause, if (state.isPlaying) "Jeda" else "Putar"
            )

            val progress = if (state.durationMs > 0L) {
                ((state.positionMs * 1000L) / state.durationMs).toInt().coerceIn(0, 1000)
            } else {
                0
            }
            views.setProgressBar(R.id.widget_progress, 1000, progress, false)

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
                if (track == null) {
                    "RexMusic. Belum ada lagu. Ketuk untuk membuka aplikasi"
                } else {
                    "$title, $artist. ${source.first.removePrefix("● ").lowercase()}. " +
                        "Ketuk untuk membuka RexMusic"
                }
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
                (if (wide) "● OFFLINE · FILE LOKAL" else "● OFFLINE") to COLOR_OFFLINE
            } else {
                (if (wide) "● ONLINE · STREAMING" else "● ONLINE") to COLOR_ONLINE
            }
        }

        private fun lyricLine(state: RexMusicUiState): String {
            if (state.nowPlaying == null) return ""
            val lyrics = state.lyrics
            return when {
                state.lyricsLoading -> "Memuat lirik…"
                lyrics.hasSynced -> {
                    val i = activeIndex(lyrics.synced, state.positionMs)
                    val text = if (i >= 0) lyrics.synced[i].text.trim() else ""
                    if (text.isBlank()) "♪" else "♪  $text"
                }
                !lyrics.isEmpty -> "Lirik tanpa sinkronisasi · buka aplikasi"
                else -> "Lirik tidak tersedia"
            }
        }

        private fun activeIndex(list: List<LyricLine>, positionMs: Long): Int {
            var lo = 0
            var hi = list.lastIndex
            var result = -1
            while (lo <= hi) {
                val mid = (lo + hi) ushr 1
                if (list[mid].timeMs <= positionMs) {
                    result = mid
                    lo = mid + 1
                } else {
                    hi = mid - 1
                }
            }
            return result
        }

        private fun formatTime(ms: Long): String {
            if (ms <= 0L) return "00:00"
            val total = ms / 1000L
            val m = total / 60
            val s = total % 60
            return "%02d:%02d".format(m, s)
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
 * Artwork widget: cover dibulatkan + warna dominan, di-cache untuk SATU lagu (kuncinya URL cover).
 */
private object WidgetArt {
    class Art(
        val url: String,
        val bitmap: Bitmap,
        val tint: Int
    )

    @Volatile
    private var current: Art? = null

    @Synchronized
    fun put(url: String, source: Bitmap) {
        if (url.isBlank() || source.isRecycled) return
        if (current?.url == url && current?.bitmap?.isRecycled == false) return
        runCatching {
            val soft: Bitmap = (if (source.config == Bitmap.Config.HARDWARE) {
                source.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                source
            }) ?: return
            current = Art(url, roundedSquare(soft), dominantTint(soft))
        }
    }

    fun forTrack(url: String?): Art? =
        current?.takeIf { !url.isNullOrBlank() && it.url == url }

    private fun roundedSquare(src: Bitmap): Bitmap {
        val size = 160
        val side = min(src.width, src.height)
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val dst = RectF(0f, 0f, size.toFloat(), size.toFloat())
        val radius = size * 0.18f
        canvas.drawRoundRect(dst, radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val crop = Rect(
            (src.width - side) / 2, (src.height - side) / 2,
            (src.width + side) / 2, (src.height + side) / 2
        )
        canvas.drawBitmap(src, crop, dst, paint)
        return out
    }

    private fun dominantTint(src: Bitmap): Int {
        val small = Bitmap.createScaledBitmap(src, 8, 8, true)
        val px = IntArray(64)
        small.getPixels(px, 0, 8, 0, 0, 8, 8)
        var r = 0L
        var g = 0L
        var b = 0L
        var n = 0
        for (p in px) {
            if ((p ushr 24) < 128) continue
            r += (p shr 16) and 0xFF
            g += (p shr 8) and 0xFF
            b += p and 0xFF
            n++
        }
        if (n == 0) return 0xFF5B47E0.toInt()
        val hsv = FloatArray(3)
        Color.colorToHSV(Color.rgb((r / n).toInt(), (g / n).toInt(), (b / n).toInt()), hsv)
        hsv[1] = (hsv[1] * 1.15f).coerceAtMost(0.9f)
        hsv[2] = hsv[2].coerceIn(0.40f, 0.85f)
        return Color.HSVToColor(hsv)
    }
}