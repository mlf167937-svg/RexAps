package com.rexaps.rexfox

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class BrowserStore(context: Context) {
    private val prefs = context.getSharedPreferences("rexfox_browser", Context.MODE_PRIVATE)

    fun bookmarks(): List<Bookmark> = runCatching {
        val json = JSONArray(prefs.getString("bookmarks", "[]"))
        buildList {
            for (i in 0 until json.length()) {
                val o = json.getJSONObject(i)
                add(Bookmark(o.getString("url"), o.optString("title"), o.optLong("id")))
            }
        }
    }.getOrDefault(emptyList())

    fun saveBookmarks(items: List<Bookmark>) {
        val json = JSONArray()
        items.forEach { b ->
            json.put(JSONObject().apply {
                put("url", b.url); put("title", b.title); put("id", b.id)
            })
        }
        prefs.edit().putString("bookmarks", json.toString()).apply()
    }

    fun history(): List<HistoryEntry> = runCatching {
        val json = JSONArray(prefs.getString("history", "[]"))
        buildList {
            for (i in 0 until json.length()) {
                val o = json.getJSONObject(i)
                add(HistoryEntry(
                    o.getString("url"),
                    o.optString("title"),
                    o.optLong("timestamp")
                ))
            }
        }.sortedByDescending { it.timestamp }
    }.getOrDefault(emptyList())

    fun saveHistory(items: List<HistoryEntry>) {
        val json = JSONArray()
        items.take(500).forEach { h ->
            json.put(JSONObject().apply {
                put("url", h.url); put("title", h.title); put("timestamp", h.timestamp)
            })
        }
        prefs.edit().putString("history", json.toString()).apply()
    }

    fun settings(): BrowserSettings = BrowserSettings(
        searchEngine = runCatching {
            SearchEngine.valueOf(
                prefs.getString("searchEngine", SearchEngine.GOOGLE.name)!!
            )
        }.getOrDefault(SearchEngine.GOOGLE),
        javaScriptEnabled = prefs.getBoolean("javascript", true),
        domStorageEnabled = prefs.getBoolean("domStorage", true),
        thirdPartyCookiesEnabled = prefs.getBoolean("thirdPartyCookies", true),
        desktopSite = prefs.getBoolean("desktopSite", false),
        doNotTrack = prefs.getBoolean("doNotTrack", false),
        trackingProtectionEnabled = prefs.getBoolean("trackingProtection", false),
        askWhereToSaveDownloads = prefs.getBoolean("askWhereToSaveDownloads", false)
    )

    fun saveSettings(s: BrowserSettings) {
        prefs.edit()
            .putString("searchEngine", s.searchEngine.name)
            .putBoolean("javascript", s.javaScriptEnabled)
            .putBoolean("domStorage", s.domStorageEnabled)
            .putBoolean("thirdPartyCookies", s.thirdPartyCookiesEnabled)
            .putBoolean("desktopSite", s.desktopSite)
            .putBoolean("doNotTrack", s.doNotTrack)
            .putBoolean("trackingProtection", s.trackingProtectionEnabled)
            .putBoolean("askWhereToSaveDownloads", s.askWhereToSaveDownloads)
            .apply()
    }
}

class BookmarkRepository(private val store: BrowserStore) {
    fun all() = store.bookmarks()
    fun isBookmarked(url: String) = all().any { it.url == url }

    fun add(url: String, title: String): Boolean {
        if (isBookmarked(url)) return false
        store.saveBookmarks(all() + Bookmark(url, title.ifBlank { url }))
        return true
    }

    fun remove(url: String) =
        store.saveBookmarks(all().filterNot { it.url == url })

    fun update(id: Long, title: String, url: String) =
        store.saveBookmarks(all().map {
            if (it.id == id) it.copy(title = title, url = url) else it
        })
}

class HistoryRepository(private val store: BrowserStore) {
    fun all() = store.history()

    fun add(url: String, title: String, incognito: Boolean) {
        if (incognito || url.isBlank() || url.startsWith("about:")) return
        store.saveHistory(
            listOf(HistoryEntry(url, title.ifBlank { url })) +
                all().filterNot { it.url == url }
        )
    }

    fun search(query: String) =
        all().filter { it.url.contains(query, true) || it.title.contains(query, true) }

    fun delete(timestamp: Long) =
        store.saveHistory(all().filterNot { it.timestamp == timestamp })

    fun clear() = store.saveHistory(emptyList())
}
