package com.rexaps.rexgames.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ChessMatchSummary(
    val id: String,
    val opponent: String,
    val result: String,
    val pgn: String,
    val finishedAt: Long
)

internal class ChessHistoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("rexgames_chess_history", Context.MODE_PRIVATE)

    fun load(): List<ChessMatchSummary> = runCatching {
        val array = JSONArray(prefs.getString(KEY_HISTORY, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                add(
                    ChessMatchSummary(
                        id = id,
                        opponent = item.optString("opponent", "Lawan"),
                        result = item.optString("result", "Selesai"),
                        pgn = item.optString("pgn", ""),
                        finishedAt = item.optLong("finishedAt", 0L)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    fun add(summary: ChessMatchSummary) {
        val current = load().filterNot { it.id == summary.id }.toMutableList()
        current.add(0, summary)
        val json = JSONArray().apply {
            current.take(MAX_ITEMS).forEach { item ->
                put(
                    JSONObject()
                        .put("id", item.id)
                        .put("opponent", item.opponent)
                        .put("result", item.result)
                        .put("pgn", item.pgn)
                        .put("finishedAt", item.finishedAt)
                )
            }
        }
        prefs.edit().putString(KEY_HISTORY, json.toString()).apply()
    }

    companion object {
        private const val KEY_HISTORY = "items_v1"
        private const val MAX_ITEMS = 25
    }
}
