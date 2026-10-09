package com.rexaps.rextools.tiktok

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.rexaps.rextools.TiktokAuthor
import com.rexaps.rextools.TiktokDownloadResult
import com.rexaps.rextools.TiktokDownloadStats
import com.rexaps.rextools.TiktokMusicInfo
import java.lang.reflect.Type

class TiktokDownloadAdapter : JsonDeserializer<TiktokDownloadResult?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): TiktokDownloadResult? {
        if (json == null || json.isJsonNull) return null
        val obj = json.asJsonObject

        return TiktokDownloadResult(
            title = obj.get("title")?.asStringOrNull(),
            taken_at = obj.get("taken_at")?.asStringOrNull(),
            region = obj.get("region")?.asStringOrNull(),
            id = obj.get("id")?.asStringOrNull(),
            duration = obj.get("duration")?.asStringOrNull(),
            cover = obj.get("cover")?.asStringOrNull(),
            size_wm = obj.get("size_wm")?.asLongOrNull(),
            size_nowm = obj.get("size_nowm")?.asLongOrNull(),
            size_nowm_hd = obj.get("size_nowm_hd")?.asLongOrNull(),
            data = obj.get("data")?.let { d ->
                when {
                    d.isJsonNull -> null
                    d.isJsonArray -> d.asJsonArray.mapNotNull { it.asStringOrNull() }.filter { it.isNotBlank() }
                    d.isJsonPrimitive -> d.asString.takeIf { it.isNotBlank() }?.let { listOf(it) }
                    else -> null
                }
            },
            music_info = obj.get("music_info")?.let {
                if (it.isJsonObject) context?.deserialize(it, TiktokMusicInfo::class.java) else null
            },
            stats = obj.get("stats")?.let {
                if (it.isJsonObject) context?.deserialize(it, TiktokDownloadStats::class.java) else null
            },
            author = obj.get("author")?.let {
                if (it.isJsonObject) context?.deserialize(it, TiktokAuthor::class.java) else null
            }
        )
    }
}

private fun JsonElement.asStringOrNull(): String? =
    if (isJsonNull) null else runCatching { asString }.getOrNull()

private fun JsonElement.asLongOrNull(): Long? {
    if (isJsonNull) return null
    return runCatching {
        val p = asJsonPrimitive
        when {
            p.isNumber -> p.asLong
            p.isString -> p.asString.toLongOrNull()
            else -> null
        }
    }.getOrNull()
}
