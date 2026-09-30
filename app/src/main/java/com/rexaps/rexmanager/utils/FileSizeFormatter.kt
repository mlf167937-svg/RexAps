package com.rexaps.rexmanager.utils

import java.text.DateFormat
import java.util.Date
import java.util.Locale

object FileSizeFormatter {
    fun format(bytes: Long): String {
        if (bytes < 0L) return "-"
        if (bytes < 1024L) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble() / 1024.0
        var index = 0
        while (value >= 1024.0 && index < units.lastIndex) {
            value /= 1024.0
            index++
        }
        return String.format(Locale.getDefault(), "%.1f %s", value, units[index])
    }
}

object DateFormatter {
    fun format(timestamp: Long): String {
        if (timestamp <= 0L) return "-"
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(timestamp))
    }
}
