package com.rexaps.rexmonitor.monitoring

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.rexaps.R
import com.rexaps.rexmonitor.RexFormat
import com.rexaps.rexmonitor.RexMetrics
import com.rexaps.rexmonitor.computeHealth

/** Seluruh logic notification dipisah dari service. Semua angka berasal dari RexMetrics aktual. */
class RexMonitorNotification(private val context: Context) {

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.rexmonitor_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.rexmonitor_channel_desc)
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    fun build(m: RexMetrics?): Notification {
        val unavailable = context.getString(R.string.rexmonitor_unavailable)
        val small = RemoteViews(context.packageName, R.layout.notification_rexmonitor_small)
        val big = RemoteViews(context.packageName, R.layout.notification_rexmonitor_big)
        val title = context.getString(R.string.rexmonitor_notif_title)

        small.setTextViewText(R.id.tv_title, title)
        big.setTextViewText(R.id.tv_title, title)

        if (m == null) {
            val starting = context.getString(R.string.rexmonitor_starting)
            small.setTextViewText(R.id.tv_summary, starting)
            big.setTextViewText(R.id.tv_status, starting)
        } else {
            val ram = m.ram
            val ramText = if (ram.usedBytes != null && ram.totalBytes != null)
                "${RexFormat.gbValue(ram.usedBytes!!)}/${RexFormat.gbValue(ram.totalBytes!!)} GB" else unavailable
            val ramPct = ram.usedPercent
            val storage = m.storage
            val storageText = if (storage.usedBytes != null && storage.totalBytes != null)
                "${RexFormat.gbValue(storage.usedBytes!!)}/${RexFormat.gbValue(storage.totalBytes!!)} GB" else unavailable
            val cpuText = RexFormat.percent(m.cpu.usagePercent) ?: unavailable
            val battery = m.battery
            val batteryText = battery.percent?.let { "$it%" + if (battery.isCharging == true) " ⚡" else "" } ?: unavailable
            val health = computeHealth(m)

            // Ringkas: hanya metrik yang tersedia.
            val summary = listOfNotNull(
                "RAM $ramText",
                m.cpu.usagePercent?.let { "CPU ${it.toInt()}%" },
                (m.temperature.cpuC ?: m.temperature.batteryC)?.let { "${it.toInt()}°C" },
                battery.percent?.let { "🔋$it%" }
            ).joinToString(" • ")
            small.setTextViewText(R.id.tv_summary, summary)

            big.setTextViewText(R.id.tv_status, health.label)
            big.setTextViewText(R.id.tv_ram, "RAM  $ramText" + (ramPct?.let { "  (${it.toInt()}%)" } ?: ""))
            big.setProgressBar(R.id.pb_ram, 100, ramPct?.toInt() ?: 0, false)
            big.setTextViewText(
                R.id.tv_storage,
                "Storage  $storageText" + (storage.usedPercent?.let { "  (${it.toInt()}%)" } ?: "")
            )
            big.setProgressBar(R.id.pb_storage, 100, storage.usedPercent?.toInt() ?: 0, false)
            big.setTextViewText(R.id.tv_cpu, "CPU  $cpuText")

            val t = m.temperature
            val tempParts = listOfNotNull(
                t.cpuC?.let { "CPU ${RexFormat.temperature(it)}" },
                t.gpuC?.let { "GPU ${RexFormat.temperature(it)}" },
                t.batteryC?.let { "Baterai ${RexFormat.temperature(it)}" }
            )
            big.setTextViewText(
                R.id.tv_temp,
                "Temperature  " + (if (tempParts.isEmpty()) unavailable else tempParts.joinToString(" • "))
            )
            big.setTextViewText(R.id.tv_battery, "Battery  $batteryText")

            val down = RexFormat.mbps(m.network.downloadBps)
            val up = RexFormat.mbps(m.network.uploadBps)
            big.setTextViewText(
                R.id.tv_net,
                "Network  " + if (down != null && up != null) "↓ $down  ↑ $up" else unavailable
            )
        }

        val open = openIntent()
        val stop = PendingIntent.getService(
            context, 1,
            Intent(context, RexMonitorService::class.java).setAction(RexMonitorService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_rexmonitor_notification)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(small)
            .setCustomBigContentView(big)
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.rexmonitor_action_open), open)
            .addAction(0, context.getString(R.string.rexmonitor_action_stop), stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun update(m: RexMetrics) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, build(m))
    }

    private fun openIntent(): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        launch.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        launch.putExtra(EXTRA_OPEN_MODULE, "rexmonitor")
        return PendingIntent.getActivity(
            context, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        const val CHANNEL_ID = "rexmonitor_monitoring"
        const val NOTIFICATION_ID = 7421
        /** Shell RexAps dapat membaca extra ini untuk langsung membuka RexMonitor. */
        const val EXTRA_OPEN_MODULE = "com.rexaps.extra.OPEN_MODULE"
    }
}
