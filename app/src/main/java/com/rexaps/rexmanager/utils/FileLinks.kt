package com.rexaps.rexmanager.utils

import java.io.File

/** API-level independent symbolic link detection (canonical path comparison). */
fun File.isSymbolicLink(): Boolean = try {
    val parent = absoluteFile.parentFile
    if (parent == null) {
        false
    } else {
        val candidate = File(parent.canonicalFile, name)
        candidate.canonicalFile != candidate.absoluteFile
    }
} catch (e: Exception) {
    false
}
