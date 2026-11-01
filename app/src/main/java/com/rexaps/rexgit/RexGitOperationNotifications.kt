package com.rexaps.rexgit

import android.Manifest
import android.app.NotificationChannel
import android.app.PendingIntent
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Lightweight progress notifications for Git operations and Actions artifact downloads. */
object RexGitOperationNotifications {
    private const val CHANNEL_ID = "rexgit_operations"
    private const val CHANNEL_NAME = "RexGit operations"
    private const val ID_BASE = 42_000

    fun progress(context: Context, key: String, title: String, detail: String, percent: Int?) {
        post(context, key, title, detail, percent, ongoing = true, failed = false)
    }

    fun completed(context: Context, key: String, title: String, detail: String) {
        post(context, key, title, detail, 100, ongoing = false, failed = false)
    }

    fun failed(context: Context, key: String, title: String, detail: String) {
        post(context, key, title, detail, null, ongoing = false, failed = true)
    }

    private fun post(
        context: Context,
        key: String,
        title: String,
        detail: String,
        percent: Int?,
        ongoing: Boolean,
        failed: Boolean
    ) {
        val app = context.applicationContext
        val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Progress clone, pull, dan unduhan GitHub Actions"
                    setShowBadge(false)
                }
            )
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                app, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        if (!NotificationManagerCompat.from(app).areNotificationsEnabled()) return

        val normalized = percent?.coerceIn(0, 100)
        val builder = NotificationCompat.Builder(app, CHANNEL_ID)
            .setSmallIcon(if (failed) android.R.drawable.stat_notify_error else android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(detail.take(180))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setContentIntent(contentIntent(app, notificationId(key)))

        if (ongoing) {
            builder.setProgress(100, normalized ?: 0, normalized == null)
            builder.setSubText(normalized?.let { "$it%" } ?: "In progress")
        } else {
            builder.setProgress(0, 0, false)
            builder.setSubText(if (failed) "Failed" else "Completed")
        }

        runCatching { manager.notify(notificationId(key), builder.build()) }
    }

    private fun contentIntent(context: Context, requestCode: Int): PendingIntent? {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            requestCode,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun notificationId(key: String): Int = ID_BASE + (key.hashCode() and 0x0007FFFF)
}
