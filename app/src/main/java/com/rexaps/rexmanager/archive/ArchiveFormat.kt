package com.rexaps.rexmanager.archive

enum class ArchiveFormat(
    val extension: String,
    val label: String,
    val description: String
) {
    ZIP("zip", "ZIP", "Compressed archive (most compatible)"),
    SEVEN_Z("7z", "7Z", "Compressed archive (LZMA2, high ratio)"),
    TAR("tar", "TAR", "Archive container only, no compression");

    companion object {
        fun fromFileName(name: String): ArchiveFormat? {
            val lower = name.lowercase()
            return values().firstOrNull { lower.endsWith("." + it.extension) }
        }
    }
}
