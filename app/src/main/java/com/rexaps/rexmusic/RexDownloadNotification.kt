package com.rexaps.rexmusic

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rexaps.R
import java.util.Locale

/**
 * Notifikasi unduhan terpisah dari media notification.
 * Layout RemoteViews hanya menggunakan widget yang didukung Android 8.1-15.
 */
object RexDownloadNotification {
    private const val TAG = "RexDownloadNotif"
    private const val CHANNEL_ID = "rexmusic_downloads"
    private const val CHANNEL_NAME = "Unduhan RexMusic"
    private const val BASE_ID = 18_000

    fun init(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Progres, ukuran, dan kecepatan unduhan lagu RexMusic"
                    setShowBadge(true)
                }
            )
        }
    }

    fun showProgress(context: Context, key: String, status: DownloadStatus) {
        post(context, key, status, terminal = false)
    }

    fun showCompleted(context: Context, key: String, status: DownloadStatus) {
        post(
            context,
            key,
            status.copy(stage = DownloadStage.Finalizing, percent = 100),
            terminal = true
        )
    }

    fun showFailed(context: Context, key: String, track: RexTrack, message: String) {
        val status = DownloadStatus(
            track = track,
            stage = DownloadStage.Finalizing,
            percent = -1
        )
        postTerminal(
            context,
            key,
            status,
            "Unduhan gagal: ${message.take(90)}",
            android.R.drawable.ic_dialog_alert
        )
    }

    fun showCancelled(context: Context, key: String, track: RexTrack) {
        val status = DownloadStatus(
            track = track,
            stage = DownloadStage.Finalizing,
            percent = -1
        )
        postTerminal(
            context,
            key,
            status,
            "Unduhan dibatalkan",
            android.R.drawable.ic_menu_close_clear_cancel
        )
    }

    private fun post(context: Context, key: String, status: DownloadStatus, terminal: Boolean) {
        val progress = if (status.percent >= 0) status.percent.coerceIn(0, 100) else 0
        val indeterminate = !terminal && (
            status.percent < 0 ||
                status.stage == DownloadStage.Resolving ||
                status.stage == DownloadStage.Queued
            )
        val stage = if (terminal) "Tersimpan offline" else stageLabel(status.stage)
        val stats = sizeLabel(status)
        val speed = speedLabel(status.speedBytesPerSecond)
        val percentLabel = if (status.percent >= 0) "$progress%" else "…"

        val compact = RemoteViews(
            context.packageName,
            R.layout.rexmusic_download_notification_compact
        ).apply {
            bindCommon(
                this, status, stage, stats, speed, percentLabel,
                progress, indeterminate
            )
        }

        val expanded = RemoteViews(
            context.packageName,
            R.layout.rexmusic_download_notification
        ).apply {
            bindCommon(
                this, status, stage, stats, speed, percentLabel,
                progress, indeterminate
            )
        }

        notify(
            context = context,
            key = key,
            compact = compact,
            expanded = expanded,
            ongoing = !terminal,
            title = status.track.title,
            contentText = "$stage · $percentLabel",
            detailText = "$stats · $speed",
            progress = progress,
            indeterminate = indeterminate
        )
    }

    private fun postTerminal(
        context: Context,
        key: String,
        status: DownloadStatus,
        message: String,
        icon: Int
    ) {
        val progress = if (status.percent >= 0) status.percent.coerceIn(0, 100) else 0
        val stats = sizeLabel(status)
        val speed = speedLabel(status.speedBytesPerSecond)
        val compact = RemoteViews(
            context.packageName,
            R.layout.rexmusic_download_notification_compact
        ).apply {
            bindCommon(
                this, status, message, stats, speed,
                if (status.percent >= 0) "$progress%" else "!",
                progress, false, iconOverride = icon
            )
        }
        val expanded = RemoteViews(
            context.packageName,
            R.layout.rexmusic_download_notification
        ).apply {
            bindCommon(
                this, status, message, stats, speed,
                if (status.percent >= 0) "$progress%" else "!",
                progress, false, iconOverride = icon
            )
        }

        notify(
            context = context,
            key = key,
            compact = compact,
            expanded = expanded,
            ongoing = false,
            title = status.track.title,
            contentText = message,
            detailText = "$stats · $speed",
            progress = progress,
            indeterminate = false
        )
    }

    private fun bindCommon(
        views: RemoteViews,
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
            R.id.download_notification_icon,
            iconOverride ?: R.drawable.ic_rexmusic_download
        )
        views.setTextViewText(R.id.download_notification_title, status.track.title)
        views.setTextViewText(R.id.download_notification_artist, status.track.artist)
        views.setTextViewText(R.id.download_notification_status, stage)
        views.setTextViewText(R.id.download_notification_stats, stats)
        views.setTextViewText(R.id.download_notification_speed, speed)
        views.setTextViewText(R.id.download_notification_percent, percentLabel)
        views.setProgressBar(
            R.id.download_notification_progress,
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
        ongoing: Boolean,
        title: String,
        contentText: String,
        detailText: String,
        progress: Int,
        indeterminate: Boolean
    ) {
        init(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "POST_NOTIFICATIONS belum diizinkan; unduhan tetap berjalan tanpa notifikasi.")
            return
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.w(TAG, "Notifikasi aplikasi dinonaktifkan di setelan Android.")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = nm.getNotificationChannel(CHANNEL_ID)
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
                Log.w(TAG, "Channel Unduhan RexMusic dinonaktifkan di setelan Android.")
                return
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_rexmusic_download)
            // Teks dan progress standar tetap tersedia sebagai fallback OS.
            .setContentTitle(title)
            .setContentText(contentText)
            .setSubText(detailText)
            .setProgress(100, progress, indeterminate)
            .setCustomContentView(compact)
            .setCustomBigContentView(expanded)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setColor(ContextCompat.getColor(context, R.color.rexmusic_download_blue))
            .setOnlyAlertOnce(true)
            .setAutoCancel(!ongoing)
            .setOngoing(ongoing)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setContentIntent(contentIntent(context))
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        try {
            nm.notify(idFor(key), builder.build())
        } catch (error: SecurityException) {
            // Misalnya izin notifikasi dicabut antara pemeriksaan dan notify().
            Log.w(TAG, "Android menolak menampilkan notifikasi unduhan.", error)
        }
    }

    private fun contentIntent(context: Context): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            19_001,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun stageLabel(stage: DownloadStage): String = when (stage) {
        DownloadStage.Queued -> "Menunggu antrean…"
        DownloadStage.Resolving -> "Menyiapkan audio…"
        DownloadStage.Downloading -> "Mengunduh audio…"
        DownloadStage.FetchingLyrics -> "Mengunduh lirik…"
        DownloadStage.Finalizing -> "Menyimpan offline…"
    }

    private fun sizeLabel(status: DownloadStatus): String = when {
        status.downloadedBytes > 0L && status.totalBytes > 0L ->
            "${formatMb(status.downloadedBytes)} MB / ${formatMb(status.totalBytes)} MB"
        status.downloadedBytes > 0L ->
            "${formatMb(status.downloadedBytes)} MB / ukuran ?"
        status.totalBytes > 0L ->
            "0.00 MB / ${formatMb(status.totalBytes)} MB"
        else ->
            "Menyiapkan ukuran…"
    }

    private fun speedLabel(bytesPerSecond: Long): String = when {
        bytesPerSecond <= 0L -> "Kecepatan —"
        bytesPerSecond < 1024L -> "$bytesPerSecond B/s"
        bytesPerSecond < 1024L * 1024L ->
            String.format(Locale.US, "%.1f KB/s", bytesPerSecond / 1024.0)
        else -> String.format(Locale.US, "%.2f MB/s", bytesPerSecond / (1024.0 * 1024.0))
    }

    private fun formatMb(bytes: Long): String =
        String.format(Locale.US, "%.2f", bytes / (1024.0 * 1024.0))

    private fun idFor(key: String): Int = BASE_ID + (key.hashCode() and 0x3FFF)
}
