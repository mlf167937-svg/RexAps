package com.rexaps.rexmusic

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

/**
 * Adapter buat field `lyrics` di response API yang kadang:
 * - OBJECT: { "plain_lyrics": "...", "synced_lyrics": "..." }
 * - STRING: "-" atau "lirik mentah"
 *
 * Kalo string, kita anggap plain_lyrics = string itu, synced_lyrics = null.
 */
class LyricsDataAdapter : JsonDeserializer<LyricsData?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): LyricsData? {
        if (json == null || json.isJsonNull) return null

        return when {
            // format 1: object
            json.isJsonObject -> {
                val obj = json.asJsonObject
                LyricsData(
                    plain_lyrics = obj.get("plain_lyrics")?.asStringOrNull(),
                    synced_lyrics = obj.get("synced_lyrics")?.asStringOrNull(),
                    duration = obj.get("duration")?.asIntOrNull()
                )
            }

            // format 2: string mentah
            json.isJsonPrimitive -> {
                val s = json.asString
                if (s.isBlank() || s == "-") null
                else LyricsData(
                    plain_lyrics = s,
                    synced_lyrics = null,
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
