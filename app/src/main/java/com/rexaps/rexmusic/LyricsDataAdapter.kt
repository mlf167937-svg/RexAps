package com.rexaps.rexmusic

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class LyricsDataAdapter : JsonDeserializer<LyricsData?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): LyricsData? {
        if (json == null || json.isJsonNull) return null

        return when {
            // format 1: object -> { "plain_lyrics": "...", "synced_lyrics": "..." }
            json.isJsonObject -> {
                val obj = json.asJsonObject
                LyricsData(
                    plainLyrics = obj.get("plain_lyrics")?.asStringOrNull(),
                    syncedLyrics = obj.get("synced_lyrics")?.asStringOrNull(),
                    duration = obj.get("duration")?.asIntOrNull()
                )
            }

            // format 2: string mentah -> "-" atau "lirik tanpa timestamp"
            json.isJsonPrimitive -> {
                val s = json.asString
                if (s.isBlank() || s == "-") null
                else LyricsData(
                    plainLyrics = s,
                    syncedLyrics = null,
                    duration = null
                )
            }

            else -> null
        }
    }
}

private fun JsonElement.asStringOrNull(): String? =
    if (isJsonNull) null else runCatching { asString }.getOrNull()

private fun JsonElement.asIntOrNull(): Int? =
    if (isJsonNull) null else runCatching { asInt }.getOrNull()
