package com.rexaps.rexcoder.runtime

import android.content.Context
import com.rexaps.rexcoder.storage.WorkspaceManager
import java.io.File
import java.util.concurrent.TimeUnit

object RexRuntime {
    private lateinit var context: Context
    private var initialized = false
    data class RuntimeSpec(val id: String, val command: String, val extensions: Set<String>)
    private val specs = listOf(
        RuntimeSpec("python", "python", setOf("py")),
        RuntimeSpec("node", "node", setOf("js", "mjs", "cjs", "ts", "tsx", "jsx"))
    )
    fun init(context: Context) { this.context = context.applicationContext; initialized = true; ensureLayout() }
    fun ensureLayout() {
        val root = File(WorkspaceManager.root, ".language"); root.mkdirs()
        File(root, "python").mkdirs(); File(root, "node").mkdirs()
    }
    fun specFor(file: File): RuntimeSpec? = specs.firstOrNull { file.extension.lowercase() in it.extensions }
    fun commandFor(file: File): List<String>? {
        val spec = specFor(file) ?: return null
        val dir = File(WorkspaceManager.root, ".language/${spec.id}")
        val active = File(dir, "active.json").takeIf { it.isFile }
        val version = active?.readText()?.substringAfter("\"version\"")?.substringAfter(':')?.trim()?.trim('"', ' ', '\n', '\r', ',')
        val base = if (!version.isNullOrBlank()) File(dir, version) else dir
        val candidates = listOf(File(base, "bin/${spec.command}"), File(base, "runtime/${spec.command}"), File(base, spec.command), File(dir, "bin/${spec.command}"), File(dir, spec.command))
        val source = candidates.firstOrNull { it.isFile }
        if (source != null && initialized) {
            val privateDir = File(context.noBackupFilesDir, "rexcoder/runtimes/${spec.id}").apply { mkdirs() }
            val target = File(privateDir, spec.command)
            if (!target.exists() || target.length() != source.length() || target.lastModified() < source.lastModified()) source.copyTo(target, overwrite = true)
            target.setExecutable(true, false)
            return listOf(target.absolutePath, file.absolutePath)
        }
        return null
    }
    fun runtimeHint(file: File): String {
        val spec = specFor(file) ?: return "No runtime registered for .${file.extension.ifBlank { "?" }}"
        return if (commandFor(file) != null) "${spec.id} runtime ready" else "${spec.id} belum terpasang. Tempatkan runtime di .language/${spec.id}"
    }
    fun run(file: File, cwd: File, timeoutMs: Long = 120_000L): Result<String> = runCatching {
        val command = commandFor(file) ?: error(runtimeHint(file))
        val p = ProcessBuilder(command).directory(cwd).redirectErrorStream(true).apply {
            environment()["REXCODER_WORKSPACE"] = WorkspaceManager.root.absolutePath
            environment()["REXCODER_LANGUAGE_HOME"] = File(WorkspaceManager.root, ".language").absolutePath
        }.start()
        val output = p.inputStream.bufferedReader().use { it.readText() }
        if (!p.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) { p.destroyForcibly(); error("Process timed out after ${timeoutMs / 1000}s") }
        "${output.trimEnd()}\n\nProcess finished with exit code ${p.exitValue()}"
    }
}
