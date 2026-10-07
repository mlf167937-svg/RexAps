package com.rexaps.rexcoder.storage

import android.os.Environment
import java.io.File

object WorkspaceManager {
    const val DISPLAY_PATH = "/storage/emulated/0/Download/RexAps/RexCoder/"
    val root: File get() = File(Environment.getExternalStorageDirectory(), "Download/RexAps/RexCoder")

    fun ensureRoot(): File = root.apply { if (!exists()) mkdirs() }

    fun isUsable(): Boolean = root.exists() && root.isDirectory && root.canRead() && root.canWrite()

    fun readTree(): com.rexaps.rexcoder.model.FileNode {
        val dir = ensureRoot()
        return node(dir, "RexCoder")
    }

    private fun node(file: File, displayName: String = file.name): com.rexaps.rexcoder.model.FileNode {
        if (file.isDirectory) {
            val children = file.listFiles()
                ?.sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))
                ?.map { node(it) }
                ?: emptyList()
            return com.rexaps.rexcoder.model.FileNode(file.absolutePath, displayName, true, children)
        }
        val content = runCatching { file.readText(Charsets.UTF_8) }.getOrDefault("")
        return com.rexaps.rexcoder.model.FileNode(file.absolutePath, displayName, false, content = content)
    }

    fun createFile(relativeName: String): File? = runCatching {
        val target = safeChild(relativeName) ?: return null
        target.parentFile?.mkdirs()
        if (!target.exists()) target.createNewFile()
        target
    }.getOrNull()

    fun createDirectory(relativeName: String): File? = runCatching {
        val target = safeChild(relativeName) ?: return null
        if (!target.exists()) target.mkdirs()
        target
    }.getOrNull()

    fun write(file: File, text: String) = file.writeText(text, Charsets.UTF_8)

    private fun safeChild(path: String): File? {
        val base = ensureRoot().canonicalFile
        val target = File(base, path).canonicalFile
        return if (target.path == base.path || target.path.startsWith(base.path + File.separator)) target else null
    }
}
