package com.rexaps.rexmusic.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import com.rexaps.rexmusic.RexMusicUiState

internal fun RexMusicWidget.Companion.__splitImpl_openAppIntent(context: Context): PendingIntent? {
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: return null
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return PendingIntent.getActivity(
                context, RC_OPEN, launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

internal fun RexMusicWidget.Companion.__splitImpl_isWide(context: Context, manager: AppWidgetManager, id: Int): Boolean {
            val options = manager.getAppWidgetOptions(id) ?: return false
            val landscape =
                context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val key = if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
            else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
            return options.getInt(key, 0) >= WIDE_MIN_DP
        }

internal fun RexMusicWidget.Companion.__splitImpl_signature(state: RexMusicUiState, art: WidgetArt.Art?, lyr: Lyr): String {
            val t = state.nowPlaying
            return listOf(
                t?.id.orEmpty(), t?.title.orEmpty(), t?.artist.orEmpty(),
                (art != null).toString(), state.isPlaying.toString(),
                state.nowPlayingOffline.toString(), state.phase.name,
                (state.durationMs / 1000L).toString(),
                lyr.cur, lyr.next
            ).joinToString("|")
        }
