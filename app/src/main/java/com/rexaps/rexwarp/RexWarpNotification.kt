package com.rexaps.rexwarp

import android.app.*
import com.rexaps.R
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

object RexWarpNotification {
    const val CHANNEL_ID = "rexwarp_status"
    const val ID = 0x7257

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "RexWARP status", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "Shows the RexWARP tunnel status"; setShowBadge(false) }
            )
        }
    }

    fun build(
        ctx: Context, connection: RexWarpConnectionState, speed: RexWarpSpeed?,
        sessionStartWallMs: Long?, pausedReason: String?
    ): Notification {
        val status = pausedReason ?: when (connection) {
            RexWarpConnectionState.CONNECTED -> "Connected"
            RexWarpConnectionState.CONNECTING -> "Connecting..."
            RexWarpConnectionState.DISCONNECTING -> "Disconnecting..."
            RexWarpConnectionState.DISCONNECTED -> "Disconnected"
            RexWarpConnectionState.ERROR -> "Error"
        }
        val live = if (connection == RexWarpConnectionState.CONNECTED && speed != null)
            "↓ ${RexWarpFormat.speed(speed.downBytesPerSec)}   ↑ ${RexWarpFormat.speed(speed.upBytesPerSec)}" else null

        val open = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.let {
            PendingIntent.getActivity(ctx, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val disconnect = PendingIntent.getService(
            ctx, 1, Intent(ctx, RexWarpVpnService::class.java).setAction(RexWarpVpnService.ACTION_DISCONNECT),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_rexwarp_notification)
            .setContentTitle("RexWARP")
            .setContentText(live ?: status)
            .setSubText(if (live != null) status else null)
            .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(open)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .apply {
                if (sessionStartWallMs != null && connection == RexWarpConnectionState.CONNECTED)
                    setUsesChronometer(true).setWhen(sessionStartWallMs).setShowWhen(true)
                if (connection != RexWarpConnectionState.DISCONNECTING)
                    addAction(0, "Disconnect", disconnect)
            }.build()
    }
}