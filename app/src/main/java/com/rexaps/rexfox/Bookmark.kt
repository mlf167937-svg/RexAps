package com.rexaps.rexfox

data class Bookmark(
    val url: String,
    val title: String,
    val id: Long = System.currentTimeMillis()
)

class BookmarkManager {
    private val bookmarks = mutableListOf<Bookmark>()

    fun add(url: String, title: String): Boolean {
        if (bookmarks.any { it.url == url }) return false
        bookmarks.add(Bookmark(url = url, title = title))
        return true
    }

    fun remove(url: String) {
        bookmarks.removeAll { it.url == url }
    }

    fun isBookmarked(url: String) = bookmarks.any { it.url == url }

    fun getAll(): List<Bookmark> = bookmarks.toList()
}
