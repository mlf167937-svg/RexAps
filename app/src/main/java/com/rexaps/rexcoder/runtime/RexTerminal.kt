package com.rexaps.rexcoder.runtime

import com.rexaps.rexcoder.storage.WorkspaceManager
import java.io.File

class RexTerminal(private val workspace: File = WorkspaceManager.root) {
    var cwd: File = workspace.canonicalFile
        private set

    fun execute(line: String): Result<String> = runCatching {
        val input = line.trim()
        if (input.isBlank()) return@runCatching ""
        val parts = tokenize(input)
        val cmd = parts.first()
        val args = parts.drop(1)
        when (cmd) {
            "cd" -> {
                val next = resolve(args.firstOrNull() ?: "~")
                requireInside(next)
                require(next.isDirectory) { "Not a directory: ${next.name}" }
                cwd = next
                ""
            }
            "pwd" -> cwd.absolutePath
            "ls" -> list(args)
            "mkdir" -> {
                requireArgs(cmd, args, 1)
                args.forEach { val f = resolve(it); requireInside(f); require(f.mkdirs() || f.isDirectory) { "mkdir failed: ${f.name}" } }
                ""
            }
            "touch" -> {
                requireArgs(cmd, args, 1)
                args.forEach { val f = resolve(it); requireInside(f); f.parentFile?.mkdirs(); if (!f.exists()) require(f.createNewFile()) { "touch failed: ${f.name}" } }
                ""
            }
            "rm" -> {
                requireArgs(cmd, args, 1)
                args.filterNot { it.startsWith("-") }.forEach { val f = resolve(it); requireInside(f); require(f.deleteRecursively()) { "rm failed: ${f.name}" } }
                ""
            }
            "rmdir" -> {
                requireArgs(cmd, args, 1)
                args.forEach { val f = resolve(it); requireInside(f); require(f.isDirectory && f.delete()) { "rmdir failed: ${f.name}" } }
                ""
            }
            "cp" -> {
                requireArgs(cmd, args, 2)
                val src = resolve(args[0]); val dst = resolve(args[1]); requireInside(src); requireInside(dst)
                require(src.exists()) { "cp: source not found" }
                if (src.isDirectory) src.copyRecursively(if (dst.isDirectory) File(dst, src.name) else dst, overwrite = true)
                else src.copyTo(if (dst.isDirectory) File(dst, src.name) else dst, overwrite = true)
                ""
            }
            "mv" -> {
                requireArgs(cmd, args, 2)
                val src = resolve(args[0]); val dst = resolve(args[1]); requireInside(src); requireInside(dst)
                require(src.renameTo(if (dst.isDirectory) File(dst, src.name) else dst)) { "mv failed" }
                ""
            }
            "cat" -> {
                requireArgs(cmd, args, 1)
                val out = StringBuilder()
                args.forEachIndexed { index, raw ->
                    if (index > 0) out.append('\n')
                    out.append(resolve(raw).readText())
                }
                out.toString()
            }
            "echo" -> {
                val out = StringBuilder()
                args.forEachIndexed { index, value ->
                    if (index > 0) out.append(' ')
                    out.append(value)
                }
                out.toString()
            }
            "clear" -> "\u000C"
            "whoami" -> "rexcoder"
            "termux-setup-storage" -> "Termux storage setup is available when RexCoder is running inside Termux. RexCoder already uses ${WorkspaceManager.DISPLAY_PATH} as its workspace."
            "ssh" -> "Use the RexCoder SSH login dialog or type the ssh command again to open it."
            "python" -> runtimeTool("python", args)
            "pip", "pip3" -> runtimeTool("python", listOf("-m", "pip") + args)
            "node" -> runtimeTool("node", args)
            "npm" -> runtimeTool("node", listOf("npm") + args, nodeNpm = true)
            "npx" -> runtimeTool("node", listOf("npx") + args, nodeNpm = true)
            "help" -> HELP
            else -> external(parts)
        }
    }

    private fun runtimeTool(id: String, args: List<String>, nodeNpm: Boolean = false): String {
        val base = File(workspace, ".language/$id")
        require(base.exists()) { "$id runtime belum tersedia di ${base.path}" }
        val executable = when (id) {
            "python" -> listOf(File(base, "active/bin/python"), File(base, "bin/python"), File(base, "python")).firstOrNull { it.isFile }
            "node" -> listOf(File(base, "active/bin/node"), File(base, "bin/node"), File(base, "node")).firstOrNull { it.isFile }
            else -> null
        } ?: error("$id runtime belum terpasang")
        val command = mutableListOf(executable.absolutePath)
        command.addAll(args)
        return external(command)
    }

    private fun list(args: List<String>): String {
        val dir = resolve(args.firstOrNull() ?: ".")
        requireInside(dir)
        require(dir.isDirectory) { "Not a directory: ${dir.name}" }

        val files = dir.listFiles()
            ?.sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()

        val out = StringBuilder()

        files.forEachIndexed { i, f ->
            if (i > 0) out.append('\n')
            out.append(if (f.isDirectory) "<DIR> " else "      ").append(f.name)
        }

        return out.toString()
    }

    private fun external(parts: List<String>): String {
        val p = ProcessBuilder(parts).directory(cwd).redirectErrorStream(true).apply {
            environment()["REXCODER_WORKSPACE"] = workspace.absolutePath
            environment()["REXCODER_LANGUAGE_HOME"] = File(workspace, ".language").absolutePath
        }.start()
        val out = p.inputStream.bufferedReader().use { it.readText() }
        val code = p.waitFor()
        return if (out.isBlank()) "Process finished with exit code $code" else out.trimEnd() + "\n\nProcess finished with exit code $code"
    }

    private fun resolve(raw: String): File {
        val p = if (raw == "~") workspace else if (raw.startsWith("/")) File(raw) else File(cwd, raw)
        return p.canonicalFile
    }

    private fun requireInside(file: File) {
        val base = workspace.canonicalFile
        require(file.path == base.path || file.path.startsWith(base.path + File.separator)) { "Path outside RexCoder workspace is blocked" }
    }

    private fun requireArgs(cmd: String, args: List<String>, n: Int) { require(args.size >= n) { "Usage: $cmd ..." } }

    private fun tokenize(s: String): List<String> {
        val out = mutableListOf<String>(); val b = StringBuilder(); var q: Char? = null; var esc = false
        for (c in s) {
            if (esc) { b.append(c); esc = false }
            else if (c == '\\') esc = true
            else if (q != null && c == q) q = null
            else if (q == null && (c == '\'' || c == '"')) q = c
            else if (q == null && c.isWhitespace()) { if (b.isNotEmpty()) { out.add(b.toString()); b.clear() } }
            else b.append(c)
        }
        if (b.isNotEmpty()) out.add(b.toString())
        return out
    }

    companion object {
        const val HELP = "Commands: cd pwd ls mkdir touch rm rmdir cp mv cat echo clear whoami termux-setup-storage ssh python pip node npm npx help\n\nPython: python file.py | pip install <package>\nNode.js: node file.js | npm install <package>\nSSH: ssh -p 22 user@host"
    }
}
