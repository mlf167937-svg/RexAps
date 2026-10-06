package com.rexaps.rexmusic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

/**
 * Notifikasi unduhan terpisah dari media notification.
 * Satu lagu = satu notification ID, sehingga progress beberapa lagu tidak saling menimpa.
 */
object RexDownloadNotification {
    private const val CHANNEL_ID = "rexmusic_downloads"
    private const val CHANNEL_NAME = "Unduhan RexMusic"
    private const val BASE_ID = 18_000

    fun init(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progress unduhan lagu dan lirik RexMusic"
                setShowBadge(true)
            }
        )
    }

    fun showProgress(context: Context, key: String, status: DownloadStatus) {
        post(context, key, status, terminal = false)
    }

    fun showCompleted(context: Context, key: String, track: RexTrack) {
        postTerminal(context, key, track, "Tersimpan untuk offline", android.R.drawable.ic_menu_save)
    }

    fun showFailed(context: Context, key: String, track: RexTrack, message: String) {
        postTerminal(context, key, track, "Gagal: ${message.take(90)}", android.R.drawable.ic_dialog_alert)
    }

    fun showCancelled(context: Context, key: String, track: RexTrack) {
        postTerminal(context, key, track, "Unduhan dibatalkan", android.R.drawable.ic_menu_close_clear_cancel)
    }

    private fun post(context: Context, key: String, status: DownloadStatus, terminal: Boolean) {
        val percent = status.percent.coerceIn(0, 100)
        val stage = when (status.stage) {
            DownloadStage.Queued -> "Masuk antrean..."
            DownloadStage.Resolving -> "Menyiapkan audio..."
            DownloadStage.Downloading -> if (status.percent >= 0) "Mengunduh audio..." else "Mengunduh audio..."
            DownloadStage.FetchingLyrics -> "Mengunduh lirik..."
            DownloadStage.Finalizing -> "Menyimpan metadata..."
        }
        val views = RemoteViews(context.packageName, resId(context, "layout", "rexmusic_download_notification")).apply {
            setImageViewResource(resId(context, "id", "download_notification_icon"), android.R.drawable.stat_sys_download)
            setTextViewText(resId(context, "id", "download_notification_title"), status.track.title)
            setTextViewText(resId(context, "id", "download_notification_status"), stage)
            setProgressBar(resId(context, "id", "download_notification_progress"), 100, percent, status.percent < 0)
            setTextViewText(resId(context, "id", "download_notification_percent"), if (status.percent >= 0) "$percent%" else "…")
        }
        notify(context, key, views, ongoing = !terminal)
    }

    private fun postTerminal(context: Context, key: String, track: RexTrack, message: String, icon: Int) {
        val views = RemoteViews(context.packageName, resId(context, "layout", "rexmusic_download_notification")).apply {
            setImageViewResource(resId(context, "id", "download_notification_icon"), icon)
            setTextViewText(resId(context, "id", "download_notification_title"), track.title)
            setTextViewText(resId(context, "id", "download_notification_status"), message)
            setProgressBar(resId(context, "id", "download_notification_progress"), 100, 100, false)
            setTextViewText(resId(context, "id", "download_notification_percent"), "✓")
        }
        notify(context, key, views, ongoing = false)
    }

    private fun notify(context: Context, key: String, views: RemoteViews, ongoing: Boolean) {
        init(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setCustomContentView(views)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setOnlyAlertOnce(true)
            .setAutoCancel(!ongoing)
            .setOngoing(ongoing)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setContentIntent(contentIntent(context))

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(idFor(key), builder.build())
    }

    private fun contentIntent(context: Context): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        intent.flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        return PendingIntent.getActivity(
            context,
            19_001,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun resId(context: Context, type: String, name: String): Int =
        context.resources.getIdentifier(name, type, context.packageName)

    private fun idFor(key: String): Int = BASE_ID + (key.hashCode() and 0x3FFF)
}
