package com.rexaps.rexmanager.utils

import com.rexaps.rexmanager.RexFile

enum class FileCategory { FOLDER, IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, CODE, APK, OTHER }

object FileTypeResolver {
    private val archiveExtensions = setOf("zip", "7z", "tar", "gz", "tgz", "xz", "bz2", "rar", "zst")
    private val codeExtensions = setOf(
        "kt", "kts", "java", "xml", "json", "js", "ts", "py", "c", "cpp", "h", "cs",
        "go", "rs", "html", "css", "sh", "gradle", "yml", "yaml", "md"
    )
    private val documentExtensions = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "txt", "rtf", "csv", "epub"
    )

    fun category(isDirectory: Boolean, extension: String?, mimeType: String?): FileCategory {
        if (isDirectory) return FileCategory.FOLDER
        val ext = extension?.lowercase()
        return when {
            ext == "apk" -> FileCategory.APK
            ext != null && ext in archiveExtensions -> FileCategory.ARCHIVE
            ext != null && ext in codeExtensions -> FileCategory.CODE
            mimeType?.startsWith("image/") == true -> FileCategory.IMAGE
            mimeType?.startsWith("video/") == true -> FileCategory.VIDEO
            mimeType?.startsWith("audio/") == true -> FileCategory.AUDIO
            (ext != null && ext in documentExtensions) || mimeType?.startsWith("text/") == true ->
                FileCategory.DOCUMENT
            else -> FileCategory.OTHER
        }
    }

    fun category(file: RexFile): FileCategory =
        category(file.isDirectory, file.extension, file.mimeType)

    fun typeLabel(isDirectory: Boolean, extension: String?, mimeType: String?): String =
        when (category(isDirectory, extension, mimeType)) {
            FileCategory.FOLDER -> "Folder"
            FileCategory.IMAGE -> "Image"
            FileCategory.VIDEO -> "Video"
            FileCategory.AUDIO -> "Audio"
            FileCategory.DOCUMENT -> "Document"
            FileCategory.ARCHIVE -> "Archive"
            FileCategory.CODE -> "Source / markup"
            FileCategory.APK -> "Android package"
            FileCategory.OTHER -> extension?.uppercase()?.let { "$it file" } ?: "File"
        }

    /** Name shown in lists; strips the last extension when [showExtension] is false. */
    fun displayName(file: RexFile, showExtension: Boolean): String {
        if (showExtension || file.isDirectory || file.extension == null) return file.name
        val dot = file.name.lastIndexOf('.')
        return if (dot > 0) file.name.substring(0, dot) else file.name
    }
}
