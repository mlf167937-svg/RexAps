package com.rexaps.rexfox

import java.text.SimpleDateFormat
import java.util.*

data class HistoryEntry(
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun formattedTime(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

class BrowserHistory {
    private val history = mutableListOf<HistoryEntry>()

    fun add(url: String, title: String = url) {
        if (url.isEmpty()) return
        if (history.lastOrNull()?.url == url) return
        history.add(HistoryEntry(url = url, title = title))
    }

    fun getAll(): List<HistoryEntry> = history.reversed()

    fun clear() = history.clear()
}
