package com.rexaps.rexmusic.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.RemoteViews
import com.rexaps.rexmusic.LyricLine
import com.rexaps.rexmusic.RexMusicService
import com.rexaps.rexmusic.RexMusicUiState
import com.rexaps.rexmusic.RexPlayerController

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
        val app = context.applicationContext

        // RemoteViews lama bisa masih tersimpan oleh launcher setelah APK diperbarui.
        // Render ulang seluruh instance supaya action/text target mengikuti layout terbaru.
        if (action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            RexPlayerController.init(app)
            updateAll(app, RexPlayerController.state.value, force = true)
            return
        }

        if (action !in CONTROL_ACTIONS) return
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

    internal data class Lyr(val cur: String, val next: String)

    companion object {
        const val ACTION_TOGGLE = "com.rexaps.rexmusic.widget.TOGGLE"
        const val ACTION_NEXT = "com.rexaps.rexmusic.widget.NEXT"
        const val ACTION_PREV = "com.rexaps.rexmusic.widget.PREV"
        const val ACTION_OPEN = "com.rexaps.rexmusic.widget.OPEN"
        const val ACTION_REFRESH = "com.rexaps.rexmusic.widget.REFRESH"

        private val CONTROL_ACTIONS =
            setOf(ACTION_TOGGLE, ACTION_NEXT, ACTION_PREV, ACTION_OPEN, ACTION_REFRESH)

        internal const val WIDE_MIN_DP = 372
        internal const val PROGRESS_STEP_MS = 1_000L

        internal const val DEFAULT_ACCENT = 0xFFA99BFF.toInt()
        internal val COLOR_ONLINE = Color.rgb(120, 200, 255)
        internal val COLOR_OFFLINE = Color.rgb(110, 235, 180)
        internal val COLOR_MUTED = Color.rgb(200, 200, 215)
        internal const val ICON_DARK = 0xFF121212.toInt()

        internal const val RC_OPEN = 10
        internal const val RC_PREV = 11
        internal const val RC_TOGGLE = 12
        internal const val RC_NEXT = 13

        internal var lastSignature = ""
        internal var lastPositionMs = -1L

        fun updateAll(
            context: Context,
            state: RexMusicUiState,
            cover: Bitmap? = null,
            coverUrl: String? = null,
            force: Boolean = false
        ) {
        return this.__splitImpl_updateAll(context, state, cover, coverUrl, force)
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

        internal fun renderInto(
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

        internal fun signature(state: RexMusicUiState, art: WidgetArt.Art?, lyr: Lyr): String {
        return this.__splitImpl_signature(state, art, lyr)
    }

        internal fun shouldRender(
            context: Context, state: RexMusicUiState, art: WidgetArt.Art?,
            lyr: Lyr, force: Boolean
        ): Boolean {
        return this.__splitImpl_shouldRender(context, state, art, lyr, force)
    }

        internal fun isWide(context: Context, manager: AppWidgetManager, id: Int): Boolean {
        return this.__splitImpl_isWide(context, manager, id)
    }

        // ---------- render ----------

        internal fun render(
            context: Context, state: RexMusicUiState, art: WidgetArt.Art?,
            lyr: Lyr, wide: Boolean
        ): RemoteViews {
        return this.__splitImpl_render(context, state, art, lyr, wide)
    }

        internal fun sourceLabel(state: RexMusicUiState, wide: Boolean): Pair<String, Int> {
        return this.__splitImpl_sourceLabel(state, wide)
    }

        internal fun lyricLines(state: RexMusicUiState): Lyr {
        return this.__splitImpl_lyricLines(state)
    }

        internal fun activeIndex(list: List<LyricLine>, positionMs: Long): Int {
            var lo = 0
            var hi = list.lastIndex
            var result = -1
            while (lo <= hi) {
                val mid = (lo + hi) ushr 1
                if (list[mid].timeMs <= positionMs) { result = mid; lo = mid + 1 } else hi = mid - 1
            }
            return result
        }

        internal fun formatTime(ms: Long): String {
            if (ms <= 0L) return "00:00"
            val total = ms / 1000L
            return "%02d:%02d".format(total / 60, total % 60)
        }

        // ---------- intents ----------

        internal fun broadcast(context: Context, action: String, request: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context, request,
                Intent(context, RexMusicWidget::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        internal fun openAppIntent(context: Context): PendingIntent? {
        return this.__splitImpl_openAppIntent(context)
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

    internal fun roundedSquare(src: Bitmap, size: Int, radiusFrac: Float): Bitmap {
        return this.__splitImpl_roundedSquare(src, size, radiusFrac)
    }

    /** Blur murah: downscale ke 32px lalu upscale + gradient gelap→warna dominan + mask sudut bulat. */
    internal fun blurBackground(src: Bitmap, w: Int, h: Int, base: Int): Bitmap {
        return this.__splitImpl_blurBackground(src, w, h, base)
    }

    internal fun averageColor(src: Bitmap): Int {
        return this.__splitImpl_averageColor(src)
    }
}