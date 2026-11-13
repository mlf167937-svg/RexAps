package com.rexaps.rexmusic

import android.content.Context
import org.json.JSONArray

/** Search-query history is deliberately separate from RexHistoryStore (played tracks). */
internal class RexMusicSearchHistoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("rex_music_search_queries", Context.MODE_PRIVATE)

    fun read(): List<String> = runCatching {
        val array = JSONArray(prefs.getString(KEY, "[]"))
        val result = ArrayList<String>(MAX_ITEMS)
        for (index in 0 until array.length()) {
            val value: String = array.optString(index).trim()
            if (value.isNotBlank() && result.none { existing -> existing.equals(value, ignoreCase = true) }) {
                result.add(value)
            }
            if (result.size == MAX_ITEMS) break
        }
        result.toList()
    }.getOrDefault(emptyList())

    fun save(query: String): List<String> {
        val clean = query.trim().replace(Regex("\\s+"), " ")
        if (clean.isBlank()) return read()
        val updated = (listOf(clean) + read().filterNot { it.equals(clean, ignoreCase = true) }).take(MAX_ITEMS)
        persist(updated)
        return updated
    }

    fun remove(query: String): List<String> = read().filterNot { it.equals(query, ignoreCase = true) }.also(::persist)

    fun clear(): List<String> = emptyList<String>().also(::persist)

    private fun persist(values: List<String>) {
        val array = JSONArray().apply { values.forEach { put(it) } }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private companion object {
        const val KEY = "queries"
        const val MAX_ITEMS = 20
    }
}
