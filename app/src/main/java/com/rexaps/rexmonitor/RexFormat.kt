package com.rexaps.rexmonitor

import java.util.Locale

object RexFormat {
    private const val KB = 1024.0
    private const val MB = KB * 1024.0
    private const val GB = MB * 1024.0

    fun gbValue(bytes: Long): String = String.format(Locale.US, "%.1f", bytes / GB)

    fun bytes(bytes: Long): String = when {
        bytes >= GB -> String.format(Locale.US, "%.1f GB", bytes / GB)
        bytes >= MB -> String.format(Locale.US, "%.0f MB", bytes / MB)
        bytes >= KB -> String.format(Locale.US, "%.0f KB", bytes / KB)
        else -> "$bytes B"
    }

    fun temperature(celsius: Float?): String? =
        celsius?.let { String.format(Locale.US, "%.1f°C", it) }

    fun percent(value: Float?): String? = value?.let { "${it.toInt().coerceIn(0, 100)}%" }

    fun ghz(khz: Long?): String? =
        khz?.let { String.format(Locale.US, "%.2f", it / 1_000_000.0) }

    /** Bits per second -> "12.6 Mb/s" (megabit, sesuai referensi desain). */
    fun mbps(bitsPerSecond: Double?): String? =
        bitsPerSecond?.let { String.format(Locale.US, "%.1f Mb/s", it / 1_000_000.0) }

    fun uptime(ms: Long): String {
        val totalMin = ms / 60_000
        val d = totalMin / 1440
        val h = (totalMin % 1440) / 60
        val m = totalMin % 60
        return if (d > 0) "${d}d ${h}h ${m}m" else "${h}h ${m}m"
    }
}
