package com.rexaps.rexmanager.utils

import java.io.File

object FileNameUtils {

    /** Returns an error message, or null when the name is valid. Pass a trimmed name. */
    fun validate(name: String, existingNames: Set<String>): String? = when {
        name.isEmpty() -> "Name can't be empty."
        name == "." || name == ".." -> "This name is not allowed."
        name.contains('/') || name.contains('\u0000') -> "Name can't contain \"/\"."
        name.toByteArray(Charsets.UTF_8).size > 255 -> "Name is too long."
        name in existingNames -> "An item with this name already exists."
        else -> null
    }

    /** Name without its last extension. ".config" stays ".config". */
    fun baseName(name: String): String {
        val dot = name.lastIndexOf('.')
        return if (dot <= 0) name else name.substring(0, dot)
    }

    /** "photo.jpg" + 1 -> "photo (1).jpg"; ".photo.jpg" + 1 -> ".photo (1).jpg". */
    fun numbered(name: String, n: Int): String {
        val dot = name.lastIndexOf('.')
        return if (dot <= 0) "$name ($n)"
        else "${name.substring(0, dot)} ($n)${name.substring(dot)}"
    }

    fun join(dir: String, name: String): String = File(dir, name).path
}
