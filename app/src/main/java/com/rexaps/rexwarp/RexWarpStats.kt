package com.rexaps.rexwarp

import java.util.Locale

data class RexWarpSpeed(val downBytesPerSec: Long = 0, val upBytesPerSec: Long = 0)

object RexWarpFormat {
    private val units = arrayOf("B", "KB", "MB", "GB", "TB")

    fun bytes(value: Long): String {
        var v = value.coerceAtLeast(0).toDouble()
        var i = 0
        while (v >= 1000 && i < units.lastIndex) { v /= 1000; i++ }
        val text = when {
            i == 0 -> v.toLong().toString()
            v < 10 -> String.format(Locale.getDefault(), "%.2f", v)
            v < 100 -> String.format(Locale.getDefault(), "%.1f", v)
            else -> String.format(Locale.getDefault(), "%.0f", v)
        }
        return "$text ${units[i]}"
    }

    fun speed(bytesPerSec: Long) = bytes(bytesPerSec) + "/s"

    fun duration(totalSeconds: Long): String {
        val s = totalSeconds.coerceAtLeast(0)
        return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    }

    /** Selisih counter kumulatif. Jika counter di-reset engine (cur < prev), pakai cur. */
    fun delta(prev: Long, cur: Long) = if (cur >= prev) cur - prev else cur
}
