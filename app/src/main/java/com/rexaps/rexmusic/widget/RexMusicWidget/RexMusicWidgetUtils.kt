package com.rexaps.rexmusic.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration

internal fun RexMusicWidget.Companion.__split_isWide_6(context: Context, manager: AppWidgetManager, id: Int): Boolean {
            val options = manager.getAppWidgetOptions(id) ?: return false
            val landscape =
                context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val key = if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
            else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
            return options.getInt(key, 0) >= WIDE_MIN_DP
        }

internal fun RexMusicWidget.Companion.__split_formatTime_11(ms: Long): String {
            if (ms <= 0L) return "00:00"
            val total = ms / 1000L
            return "%02d:%02d".format(total / 60, total % 60)
        }
