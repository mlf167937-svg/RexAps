package com.rexaps.rexmonitor.monitoring

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.rexaps.R
import com.rexaps.rexmonitor.RexFormat
import com.rexaps.rexmonitor.RexMetrics
import com.rexaps.rexmonitor.RexHealthLevel
import com.rexaps.rexmonitor.computeHealth
import com.rexaps.rexmonitor.device.RexThermalLevel

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
        val small = RemoteViews(context.packageName, R.layout.notification_rexmonitor_small)
        val big = RemoteViews(context.packageName, R.layout.notification_rexmonitor_big)
        val title = brandTitle()
        small.setTextViewText(R.id.tv_title, title)
        big.setTextViewText(R.id.tv_title, title)

        if (m == null) {
            val starting = context.getString(R.string.rexmonitor_starting)
            small.setTextViewText(R.id.tv_summary, starting)
            big.setTextViewText(R.id.tv_status, starting)
        } else {
            bind(small, big, m)
        }

        val open = openIntent()
        val stop = PendingIntent.getService(
            context, 1,
            Intent(context, RexMonitorService::class.java).setAction(RexMonitorService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_rexmonitor_notification)
            .setColor(COLOR_CYAN)
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

    /** "RexMONITOR  PRO" dengan warna merek. */
    private fun brandTitle(): CharSequence {
        val sb = SpannableStringBuilder()
        sb.append("Rex")
        val a = sb.length
        sb.append("MONITOR")
        sb.setSpan(ForegroundColorSpan(COLOR_CYAN), a, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val b = sb.length
        sb.append("  PRO")
        sb.setSpan(ForegroundColorSpan(COLOR_PURPLE), b, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(RelativeSizeSpan(0.7f), b, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return sb
    }

    /** Semua nilai berasal dari RexMetrics aktual; yang tidak ada ditampilkan "—"/Unavailable. */
    private fun bind(small: RemoteViews, big: RemoteViews, m: RexMetrics) {
        val na = "—"
        val unavailable = context.getString(R.string.rexmonitor_unavailable)
        val health = computeHealth(m)

        // Header: status memakai simbol + teks + warna (tidak hanya warna).
        val (symbol, color) = when (health.level) {
            RexHealthLevel.NORMAL -> "✔" to COLOR_GREEN
            RexHealthLevel.WARM -> "▲" to COLOR_ORANGE
            RexHealthLevel.ATTENTION -> "✖" to COLOR_RED
        }
        big.setTextViewText(R.id.tv_status, "$symbol ${health.label}")
        big.setTextColor(R.id.tv_status, color)

        val battery = m.battery
        val batteryChip = battery.percent?.let { "$it%" + if (battery.isCharging == true) " ⚡" else "" } ?: na
        big.setTextViewText(R.id.tv_battery, batteryChip)
        small.setTextViewText(R.id.tv_battery, batteryChip)

        // Ringkasan collapsed: hanya metrik yang tersedia.
        val ram = m.ram
        val ramText = if (ram.usedBytes != null && ram.totalBytes != null)
            "${RexFormat.gbValue(ram.usedBytes!!)}/${RexFormat.gbValue(ram.totalBytes!!)} GB" else null
        small.setTextViewText(
            R.id.tv_summary,
            listOfNotNull(
                ramText?.let { "RAM $it" },
                m.cpu.usagePercent?.let { "CPU ${it.toInt()}%" },
                (m.temperature.cpuC ?: m.temperature.batteryC)?.let { "${it.toInt()}°C" }
            ).joinToString(" • ").ifBlank { health.label }
        )

        // RAM
        val ramPct = ram.usedPercent
        big.setTextViewText(R.id.tv_ram_value, ramText ?: na)
        setBar(big, R.id.pb_ram, ramPct)
        big.setTextViewText(
            R.id.tv_ram_sub,
            listOfNotNull(
                RexFormat.percent(ramPct),
                ram.availableBytes?.let { "${RexFormat.gbValue(it)} GB sisa" }
            ).joinToString(" • ").ifBlank { unavailable }
        )

        // Storage
        val st = m.storage
        val stPct = st.usedPercent
        big.setTextViewText(
            R.id.tv_storage_value,
            if (st.usedBytes != null && st.totalBytes != null)
                "${RexFormat.gbValue(st.usedBytes!!)}/${RexFormat.gbValue(st.totalBytes!!)} GB" else na
        )
        setBar(big, R.id.pb_storage, stPct)
        big.setTextViewText(
            R.id.tv_storage_sub,
            listOfNotNull(
                RexFormat.percent(stPct),
                st.freeBytes?.let { "${RexFormat.gbValue(it)} GB sisa" }
            ).joinToString(" • ").ifBlank { unavailable }
        )

        // CPU: frekuensi selalu bila terbaca; bar hanya bila usage benar-benar terhitung.
        val cpu = m.cpu
        big.setTextViewText(R.id.tv_cpu_value, RexFormat.ghz(cpu.currentFreqKhz)?.let { "$it GHz" } ?: na)
        setBar(big, R.id.pb_cpu, cpu.usagePercent)
        big.setTextViewText(
            R.id.tv_cpu_sub,
            cpu.usagePercent?.let { "${it.toInt()}% • ${cpu.coreCount} core" } ?: "Usage $unavailable"
        )

        // Suhu: label jujur sesuai sumber (CPU bila terbaca, kalau tidak baterai).
        val t = m.temperature
        val main: Float?
        val mainLabel: String
        if (t.cpuC != null) {
            main = t.cpuC; mainLabel = "SUHU CPU"
        } else {
            main = t.batteryC; mainLabel = "SUHU BATERAI"
        }
        big.setTextViewText(R.id.tv_temp_label, mainLabel)
        big.setTextViewText(R.id.tv_temp_value, RexFormat.temperature(main) ?: na)
        val others = listOfNotNull(
            t.gpuC?.let { "GPU ${it.toInt()}°" },
            if (t.cpuC != null) t.batteryC?.let { "Bat ${it.toInt()}°" } else null
        )
        big.setTextViewText(
            R.id.tv_temp_sub,
            others.joinToString(" • ").ifBlank { thermalWord(t.thermalLevel) ?: unavailable }
        )

        // Jaringan: total traffic perangkat ("Device Network"), bukan RexWARP.
        val n = m.network
        big.setTextViewText(R.id.tv_net_down, "↓ " + (RexFormat.mbps(n.downloadBps) ?: na))
        big.setTextViewText(R.id.tv_net_up, "↑ " + (RexFormat.mbps(n.uploadBps) ?: na))

        // GPU
        val gpu = m.gpu
        big.setTextViewText(R.id.tv_gpu_value, gpu.renderer ?: na)
        big.setTextViewText(
            R.id.tv_gpu_sub,
            gpu.usagePercent?.let { "$it%" } ?: "Usage $unavailable"
        )
    }

    private fun setBar(v: RemoteViews, id: Int, percent: Float?) {
        if (percent == null) {
            v.setViewVisibility(id, View.INVISIBLE)
        } else {
            v.setViewVisibility(id, View.VISIBLE)
            v.setProgressBar(id, 100, percent.toInt().coerceIn(0, 100), false)
        }
    }

    private fun thermalWord(level: RexThermalLevel?): String? = when (level) {
        null -> null
        RexThermalLevel.NONE -> "Termal normal"
        RexThermalLevel.LIGHT, RexThermalLevel.MODERATE -> "Termal naik"
        else -> "Termal tinggi"
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
        private val COLOR_CYAN = Color.parseColor("#18E0FF")
        private val COLOR_PURPLE = Color.parseColor("#8A4DFF")
        private val COLOR_GREEN = Color.parseColor("#21E6A1")
        private val COLOR_ORANGE = Color.parseColor("#FFA52F")
        private val COLOR_RED = Color.parseColor("#FF4D5E")

        const val CHANNEL_ID = "rexmonitor_monitoring"
        const val NOTIFICATION_ID = 7421
        /** Shell RexAps dapat membaca extra ini untuk langsung membuka RexMonitor. */
        const val EXTRA_OPEN_MODULE = "com.rexaps.extra.OPEN_MODULE"
    }
}
