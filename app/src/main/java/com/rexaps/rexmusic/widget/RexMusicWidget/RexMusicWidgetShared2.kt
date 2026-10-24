package com.rexaps.rexmusic.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

internal fun RexMusicWidget.Companion.__split_broadcast_12(context: Context, action: String, request: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context, request,
                Intent(context, RexMusicWidget::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

internal fun RexMusicWidget.Companion.__split_openAppIntent_13(context: Context): PendingIntent? {
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: return null
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return PendingIntent.getActivity(
                context, RC_OPEN, launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

internal fun RexMusicWidget.Companion.__split_openApp_14(context: Context) {
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: return
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            runCatching { context.startActivity(launch) }
        }
