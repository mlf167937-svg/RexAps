package com.rexaps.rexmusic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import java.util.Locale

/**
 * Notifikasi unduhan terpisah dari media notification.
 *
 * Satu lagu = satu notification ID. Notification ini sengaja memakai RemoteViews
 * sendiri supaya progress, ukuran file, dan speed punya hierarchy visual yang jelas.
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

    fun showCompleted(context: Context, key: String, status: DownloadStatus) {
        post(context, key, status.copy(stage = DownloadStage.Finalizing, percent = 100), terminal = true)
    }

    fun showFailed(context: Context, key: String, track: RexTrack, message: String) {
        val status = DownloadStatus(
            track = track,
            stage = DownloadStage.Finalizing,
            percent = -1
        )
        postTerminal(context, key, status, "Gagal: ${message.take(90)}", android.R.drawable.ic_dialog_alert)
    }

    fun showCancelled(context: Context, key: String, track: RexTrack) {
        val status = DownloadStatus(
            track = track,
            stage = DownloadStage.Finalizing,
            percent = -1
        )
        postTerminal(context, key, status, "Unduhan dibatalkan", android.R.drawable.ic_menu_close_clear_cancel)
    }

    private fun post(context: Context, key: String, status: DownloadStatus, terminal: Boolean) {
        val progress = if (status.percent >= 0) status.percent.coerceIn(0, 100) else 0
        val indeterminate = status.percent < 0 || status.stage == DownloadStage.Resolving || status.stage == DownloadStage.Queued
        val stage = stageLabel(status.stage)
        val stats = sizeLabel(status)
        val speed = speedLabel(status.speedBytesPerSecond)
        val percentLabel = if (status.percent >= 0) "$progress%" else "…"

        val compact = RemoteViews(
            context.packageName,
            resId(context, "layout", "rexmusic_download_notification_compact")
        ).apply {
            bindCommon(this, context, status, stage, stats, speed, percentLabel, progress, indeterminate)
        }

        val expanded = RemoteViews(
            context.packageName,
            resId(context, "layout", "rexmusic_download_notification")
        ).apply {
            bindCommon(this, context, status, stage, stats, speed, percentLabel, progress, indeterminate)
        }

        notify(context, key, compact, expanded, ongoing = !terminal)
    }

    private fun postTerminal(
        context: Context,
        key: String,
        status: DownloadStatus,
        message: String,
        icon: Int
    ) {
        val downloaded = status.downloadedBytes
        val total = status.totalBytes
        val progress = if (status.percent >= 0) status.percent.coerceIn(0, 100) else 0
        val stats = when {
            downloaded > 0L && total > 0L -> "${formatMb(downloaded)} MB / ${formatMb(total)} MB"
            downloaded > 0L -> "${formatMb(downloaded)} MB / ? MB"
            else -> "Ukuran belum diketahui"
        }
        val speed = speedLabel(status.speedBytesPerSecond)

        val compact = RemoteViews(
            context.packageName,
            resId(context, "layout", "rexmusic_download_notification_compact")
        ).apply {
            bindCommon(
                this,
                context,
                status,
                message,
                stats,
                speed,
                if (progress > 0) "$progress%" else "!",
                progress,
                false,
                iconOverride = icon
            )
        }

        val expanded = RemoteViews(
            context.packageName,
            resId(context, "layout", "rexmusic_download_notification")
        ).apply {
            bindCommon(
                this,
                context,
                status,
                message,
                stats,
                speed,
                if (progress > 0) "$progress%" else "!",
                progress,
                false,
                iconOverride = icon
            )
        }

        notify(context, key, compact, expanded, ongoing = false)
    }

    private fun bindCommon(
        views: RemoteViews,
        context: Context,
        status: DownloadStatus,
        stage: String,
        stats: String,
        speed: String,
        percentLabel: String,
        progress: Int,
        indeterminate: Boolean,
        iconOverride: Int? = null
    ) {
        views.setImageViewResource(
            resId(context, "id", "download_notification_icon"),
            iconOverride ?: resId(context, "drawable", "ic_rexmusic_download")
        )
        views.setTextViewText(
            resId(context, "id", "download_notification_title"),
            status.track.title
        )
        views.setTextViewText(
            resId(context, "id", "download_notification_artist"),
            status.track.artist
        )
        views.setTextViewText(
            resId(context, "id", "download_notification_status"),
            stage
        )
        views.setTextViewText(
            resId(context, "id", "download_notification_stats"),
            stats
        )
        views.setTextViewText(
            resId(context, "id", "download_notification_speed"),
            speed
        )
        views.setTextViewText(
            resId(context, "id", "download_notification_percent"),
            percentLabel
        )
        views.setProgressBar(
            resId(context, "id", "download_notification_progress"),
            100,
            progress,
            indeterminate
        )
    }

    private fun notify(
        context: Context,
        key: String,
        compact: RemoteViews,
        expanded: RemoteViews,
        ongoing: Boolean
    ) {
        init(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(resId(context, "drawable", "ic_rexmusic_download"))
            .setCustomContentView(compact)
            .setCustomBigContentView(expanded)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setOnlyAlertOnce(true)
            .setAutoCancel(!ongoing)
            .setOngoing(ongoing)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setContentIntent(contentIntent(context))
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

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

    private fun stageLabel(stage: DownloadStage): String = when (stage) {
        DownloadStage.Queued -> "Menunggu antrean..."
        DownloadStage.Resolving -> "Menyiapkan audio..."
        DownloadStage.Downloading -> "Mengunduh audio..."
        DownloadStage.FetchingLyrics -> "Mengunduh lyrics..."
        DownloadStage.Finalizing -> "Menyimpan offline..."
    }

    private fun sizeLabel(status: DownloadStatus): String = when {
        status.downloadedBytes > 0L && status.totalBytes > 0L ->
            "${formatMb(status.downloadedBytes)} MB / ${formatMb(status.totalBytes)} MB"
        status.downloadedBytes > 0L ->
            "${formatMb(status.downloadedBytes)} MB / ? MB"
        status.totalBytes > 0L ->
            "0.0 MB / ${formatMb(status.totalBytes)} MB"
        else ->
            "Menyiapkan ukuran..."
    }

    private fun speedLabel(bytesPerSecond: Long): String = when {
        bytesPerSecond <= 0L -> "-- MB/s"
        else -> "${formatMb(bytesPerSecond)} MB/s"
    }

    private fun formatMb(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.2f", mb)
    }

    private fun resId(context: Context, type: String, name: String): Int =
        context.resources.getIdentifier(name, type, context.packageName)

    private fun idFor(key: String): Int = BASE_ID + (key.hashCode() and 0x3FFF)
}
