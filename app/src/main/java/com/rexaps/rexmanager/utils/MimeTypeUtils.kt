package com.rexaps.rexmanager.utils

import android.webkit.MimeTypeMap

object MimeTypeUtils {
    /** Lowercase extension without the dot, or null ("photo.JPG" -> "jpg", ".gitignore" -> null). */
    fun extensionOf(name: String): String? {
        val dot = name.lastIndexOf('.')
        return if (dot <= 0 || dot == name.lastIndex) null else name.substring(dot + 1).lowercase()
    }

    fun fromExtension(extension: String?): String? {
        val ext = extension?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    }
}
