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
import android.graphics.Color
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
