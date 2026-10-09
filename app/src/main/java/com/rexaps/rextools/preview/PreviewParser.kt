package com.rexaps.rextools.preview

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException

object PreviewParser {

    private val URL_REGEX = Regex("""https?://[^\s"'<>\]}\\)]+""", RegexOption.IGNORE_CASE)

    private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "avif")
    private val VIDEO_EXT = setOf("mp4", "webm", "mov", "mkv", "m4v", "3gp", "avi")

    /** Extract dari input apapun (JSON, text campur, URL tunggal). */
    fun parse(input: String): List<PreviewItem> {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return emptyList()

        val urls: List<String> = when {
            trimmed.startsWith("{") || trimmed.startsWith("[") -> extractFromJson(trimmed)
                ?: extractByRegex(trimmed)
            else -> extractByRegex(trimmed)
        }

        return urls.asSequence()
            .map { it.trim().trimEnd(',', '.', ';', ')', ']', '}') }
            .filter { it.startsWith("http://", true) || it.startsWith("https://", true) }
            .distinct()
            .map { url -> PreviewItem(url = url, type = detectType(url)) }
            .toList()
    }

    private fun extractFromJson(input: String): List<String>? = try {
        val root = JsonParser.parseString(input)
        val out = mutableListOf<String>()
        collectStrings(root, out)
        out
    } catch (_: JsonSyntaxException) {
        null
    } catch (_: Exception) {
        null
    }

    private fun collectStrings(elem: JsonElement?, out: MutableList<String>) {
        if (elem == null || elem.isJsonNull) return
        when {
            elem.isJsonPrimitive -> {
                val s = elem.asString
                if (s.startsWith("http://", true) || s.startsWith("https://", true)) {
                    out += s
                } else {
                    // kadang url ke-embed di string lain
                    URL_REGEX.findAll(s).forEach { out += it.value }
                }
            }
            elem.isJsonArray -> elem.asJsonArray.forEach { collectStrings(it, out) }
            elem.isJsonObject -> elem.asJsonObject.entrySet().forEach { collectStrings(it.value, out) }
        }
    }

    private fun extractByRegex(input: String): List<String> =
        URL_REGEX.findAll(input).map { it.value }.toList()

    /** Detect tipe media dari extension (sebelum query string). */
    fun detectType(url: String): MediaType {
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        val ext = path.substringAfterLast('.', "").takeIf { it.length in 2..5 }
            ?: return MediaType.UNKNOWN

        return when (ext) {
            in IMAGE_EXT -> MediaType.IMAGE
            in VIDEO_EXT -> MediaType.VIDEO
            else -> MediaType.UNKNOWN
        }
    }
}
