package com.rexaps.rexmusic.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.RemoteViews
import com.rexaps.R
import com.rexaps.rexmusic.RexMusicUiState
import com.rexaps.rexmusic.RexPlayerController

/** Home-screen playback card. Widget updates are event/second based; Android limits continuous animation in widgets. */
class RexMusicWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        RexPlayerController.init(context.applicationContext)
        val state = RexPlayerController.state.value
        ids.forEach { id -> manager.updateAppWidget(id, render(context, state, null)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        RexPlayerController.init(context.applicationContext)
        when (intent.action) {
            ACTION_TOGGLE -> RexPlayerController.togglePlay()
            ACTION_NEXT -> RexPlayerController.next()
            ACTION_PREV -> RexPlayerController.prev(force = true)
            ACTION_OPEN -> {
                val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                launch?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                if (launch != null) context.startActivity(launch)
            }
            ACTION_REFRESH -> updateAll(context, RexPlayerController.state.value, null)
        }
        updateAll(context, RexPlayerController.state.value, null)
    }

    companion object {
        const val ACTION_TOGGLE = "com.rexaps.rexmusic.widget.TOGGLE"
        const val ACTION_NEXT = "com.rexaps.rexmusic.widget.NEXT"
        const val ACTION_PREV = "com.rexaps.rexmusic.widget.PREV"
        const val ACTION_OPEN = "com.rexaps.rexmusic.widget.OPEN"
        const val ACTION_REFRESH = "com.rexaps.rexmusic.widget.REFRESH"

        fun updateAll(context: Context, state: RexMusicUiState, cover: Bitmap?) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, RexMusicWidget::class.java))
            if (ids.isEmpty()) return
            val views = render(context, state, cover)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun pending(context: Context, action: String, request: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context, request, Intent(context, RexMusicWidget::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        private fun render(context: Context, state: RexMusicUiState, cover: Bitmap?): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_rexmusic_player)
            val track = state.nowPlaying
            views.setTextViewText(R.id.widget_track_title, track?.title ?: "RexMusic")
            views.setTextViewText(R.id.widget_track_artist, track?.artist ?: "Pilih lagu untuk mulai")
            views.setTextViewText(
                R.id.widget_source,
                when {
                    track == null -> "PLAYER SIAP"
                    state.nowPlayingOffline -> "● OFFLINE · FILE LOKAL"
                    else -> "● ONLINE · STREAMING"
                }
            )
            views.setTextColor(R.id.widget_source, when {
                track == null -> Color.LTGRAY
                state.nowPlayingOffline -> Color.rgb(96, 230, 170)
                else -> Color.rgb(95, 190, 255)
            })
            val lyric = currentLyric(state)
            views.setTextViewText(R.id.widget_lyrics, lyric.ifBlank { "♪  Lirik belum tersedia" })
            views.setViewVisibility(R.id.widget_lyrics, android.view.View.VISIBLE)
            views.setBoolean(R.id.widget_lyrics, "setSelected", true)
            views.setTextViewText(R.id.widget_play_pause, if (state.isPlaying) "Ⅱ" else "▶")
            views.setProgressBar(
                R.id.widget_progress,
                100,
                if (state.durationMs > 0) ((state.positionMs * 100L) / state.durationMs).toInt().coerceIn(0, 100) else 0,
                false
            )
            if (cover != null) {
                views.setImageViewBitmap(R.id.widget_cover, cover)
            } else if (track == null) {
                views.setImageViewResource(R.id.widget_cover, R.mipmap.ic_launcher_round)
            }
            views.setOnClickPendingIntent(R.id.widget_root, pending(context, ACTION_OPEN, 10))
            views.setOnClickPendingIntent(R.id.widget_prev, pending(context, ACTION_PREV, 11))
            views.setOnClickPendingIntent(R.id.widget_play_pause, pending(context, ACTION_TOGGLE, 12))
            views.setOnClickPendingIntent(R.id.widget_next, pending(context, ACTION_NEXT, 13))
            return views
        }

        private fun currentLyric(state: RexMusicUiState): String {
            val synced = state.lyrics.synced
            if (synced.isNotEmpty()) {
                val index = synced.indexOfLast { it.timeMs <= state.positionMs }
                return synced.getOrNull(index)?.text.orEmpty()
            }
            return state.lyrics.plain.lineSequence().map(String::trim).firstOrNull(String::isNotBlank).orEmpty()
        }
    }
}
