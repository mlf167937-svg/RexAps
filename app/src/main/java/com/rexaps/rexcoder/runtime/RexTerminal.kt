package com.rexaps.rexcoder.runtime

import com.rexaps.rexcoder.storage.WorkspaceManager
import java.io.File

class RexTerminal(private val workspace: File = WorkspaceManager.root) {
    var cwd: File = workspace.canonicalFile
        private set

    fun execute(line: String): Result<String> = runCatching {
        val input = line.trim(); if (input.isBlank()) return@runCatching ""
        val parts = tokenize(input); val cmd = parts.first(); val args = parts.drop(1)
        when (cmd) {
            "cd" -> { val next = resolve(args.firstOrNull() ?: "~"); requireInside(next); require(next.isDirectory) { "Not a directory: ${next.name}" }; cwd = next; "" }
            "pwd" -> cwd.absolutePath
            "ls" -> list(args)
            "mkdir" -> { requireArgs(cmd, args, 1); args.forEach { val f = resolve(it); requireInside(f); require(f.mkdirs() || f.isDirectory) { "mkdir failed: ${f.name}" } }; "" }
            "touch" -> { requireArgs(cmd, args, 1); args.forEach { val f = resolve(it); requireInside(f); f.parentFile?.mkdirs(); if (!f.exists()) require(f.createNewFile()) { "touch failed: ${f.name}" } }; "" }
            "rm" -> { requireArgs(cmd, args, 1); args.filterNot { it.startsWith("-") }.forEach { val f = resolve(it); requireInside(f); require(f.deleteRecursively() || !f.exists()) { "rm failed: ${f.name}" } }; "" }
            "rmdir" -> { requireArgs(cmd, args, 1); args.forEach { val f = resolve(it); requireInside(f); require(!f.exists() || f.delete()) { "rmdir failed: ${f.name}" } }; "" }
            "cp" -> copy(args)
            "mv" -> move(args)
            "cat" -> { requireArgs(cmd, args, 1); readFiles(args) }
            "echo" -> args.joinToString(" ")
            "clear" -> "\u000C"
            "whoami" -> "rexcoder"
            "rex" -> rex(args)
            "pip", "pip3" -> runtimeTool("python", listOf("-m", "pip") + args)
            "npm" -> runtimeTool("node", listOf("npm") + args, nodeNpm = true)
            "npx" -> runtimeTool("node", listOf("npx") + args, nodeNpm = true)
            "run" -> { requireArgs(cmd, args, 1); external(args) }
            "help" -> HELP
            else -> external(parts)
        }
    }

    private fun rex(args: List<String>): String {
        if (args.isEmpty()) return REX_HELP
        return when (args[0]) {
            "install" -> { requireArgs("rex install", args.drop(1), 1); RexPackageManager.install(args[1], args.getOrNull(2)).getOrElse { throw it } }
            "list" -> RexPackageManager.listInstalled().let { installed -> if (installed.isEmpty()) "No runtimes installed" else installed.joinToString("\n") }
            "registry" -> when (args.getOrNull(1)) {
                "set" -> { val url = args.getOrNull(2) ?: error("Usage: rex registry set <url>"); RexPackageManager.setRegistryUrl(url); "Registry URL updated" }
                "show", null -> "Registry: ${RexPackageManager.registryUrl().ifBlank { "local (.rex/registry.json)" }}"
                else -> error("Usage: rex registry set <url> | rex registry show")
            }
            "help" -> REX_HELP
            else -> error("Unknown rex command: ${args[0]}")
        }
    }

    private fun runtimeTool(id: String, args: List<String>, nodeNpm: Boolean = false): String {
        val dir = File(WorkspaceManager.root, ".language/$id")
        val active = File(dir, "active.json").takeIf { it.isFile }
        val version = active?.readText()?.substringAfter("\"version\"")?.substringAfter(':')?.trim()?.trim('"', ' ', '\n', '\r', ',')
        val base = if (!version.isNullOrBlank()) File(dir, version) else dir
        val command = if (id == "python") {
            val python = listOf(File(base, "bin/python"), File(base, "python"), File(dir, "bin/python")).firstOrNull { it.isFile }?.absolutePath ?: "python"
            listOf(python) + args
        } else {
            val node = listOf(File(base, "bin/node"), File(base, "node"), File(dir, "bin/node")).firstOrNull { it.isFile }?.absolutePath ?: "node"
            if (nodeNpm) {
                val tool = if (args.isNotEmpty() && args[0] == "npm") "npm-cli.js" else "npx-cli.js"
                val cli = listOf(File(base, "bin/$tool"), File(base, tool), File(dir, "bin/$tool")).firstOrNull { it.isFile }
                if (cli != null) listOf(node, cli.absolutePath) + args.drop(1) else listOf(if (tool == "npm-cli.js") "npm" else "npx") + args.drop(1)
            } else listOf(node) + args
        }
        return external(command)
    }

    private fun list(args: List<String>): String {
        val dir = args.firstOrNull()?.let(::resolve) ?: cwd
        requireInside(dir)
        val files = dir.listFiles()?.sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
        val out = StringBuilder()
        for (file in files) {
            if (out.isNotEmpty()) out.append('\n')
            out.append(if (file.isDirectory) file.name + "/" else file.name)
        }
        return out.toString()
    }
    private fun readFiles(args: List<String>): String {
        val out = StringBuilder()
        for (path in args) {
            if (out.isNotEmpty()) out.append('\n')
            out.append(resolve(path).readText())
        }
        return out.toString()
    }

    private fun copy(args: List<String>): String { requireArgs("cp", args, 2); val src = resolve(args[0]); val dst = resolve(args[1]); requireInside(src); requireInside(dst); if (src.isDirectory) src.copyRecursively(dst, true) else src.copyTo(if (dst.isDirectory) File(dst, src.name) else dst, true); return "" }
    private fun move(args: List<String>): String { requireArgs("mv", args, 2); val src = resolve(args[0]); val dst = resolve(args[1]); requireInside(src); requireInside(dst); require(src.renameTo(if (dst.isDirectory) File(dst, src.name) else dst)) { "move failed" }; return "" }
    private fun external(parts: List<String>): String { val p = ProcessBuilder(parts).directory(cwd).redirectErrorStream(true).apply { environment()["REXCODER_WORKSPACE"] = workspace.absolutePath }.start(); val out = p.inputStream.bufferedReader().use { it.readText() }; val code = p.waitFor(); return if (out.isBlank()) "Process finished with exit code $code" else out.trimEnd() + "\n\nProcess finished with exit code $code" }
    private fun resolve(raw: String): File { val p = if (raw == "~") workspace else if (raw.startsWith("/")) File(raw) else File(cwd, raw); return p.canonicalFile }
    private fun requireInside(file: File) { val base = workspace.canonicalFile; require(file.path == base.path || file.path.startsWith(base.path + File.separator)) { "Path outside RexCoder workspace is blocked" } }
    private fun requireArgs(cmd: String, args: List<String>, n: Int) { require(args.size >= n) { "Usage: $cmd ..." } }
    private fun tokenize(s: String): List<String> { val out = mutableListOf<String>(); val b = StringBuilder(); var q: Char? = null; var esc = false; for (c in s) { if (esc) { b.append(c); esc = false } else if (c == '\\') esc = true else if (q != null && c == q) q = null else if (q == null && (c == '\'' || c == '"')) q = c else if (q == null && c.isWhitespace()) { if (b.isNotEmpty()) { out += b.toString(); b.clear() } } else b.append(c) }; if (b.isNotEmpty()) out += b.toString(); return out }
    companion object {
        const val HELP = "Commands: cd pwd ls mkdir touch rm rmdir cp mv cat echo clear rex python pip node npm run help\nInstall runtime: rex install python | rex install node\nPython packages: pip install <package>\nNode packages: npm install <package>"
        const val REX_HELP = "rex install <python|node> [version]\nrex list\nrex registry show\nrex registry set <url>"
    }
}
