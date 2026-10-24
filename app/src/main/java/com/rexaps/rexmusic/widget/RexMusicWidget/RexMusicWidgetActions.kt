package com.rexaps.rexmusic.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import com.rexaps.rexmusic.RexMusicUiState

internal fun RexMusicWidget.Companion.__splitImpl_updateAll(
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
