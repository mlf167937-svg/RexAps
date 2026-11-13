package com.rexaps.rexgames.data

import android.content.Context
import com.rexaps.rexgames.catur.bot.ChessDifficulty

internal class ChessSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("rexgames_chess_settings", Context.MODE_PRIVATE)

    fun difficulty(): ChessDifficulty = runCatching {
        ChessDifficulty.valueOf(prefs.getString(KEY_DIFFICULTY, ChessDifficulty.MEDIUM.name).orEmpty())
    }.getOrDefault(ChessDifficulty.MEDIUM)

    fun saveDifficulty(value: ChessDifficulty) {
        prefs.edit().putString(KEY_DIFFICULTY, value.name).apply()
    }

    companion object { private const val KEY_DIFFICULTY = "difficulty_v1" }
}
