package com.rexaps.rexcoder.model

import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.rexaps.rexcoder.storage.WorkspaceManager
import com.rexaps.rexcoder.runtime.AnsiText
import com.rexaps.rexcoder.runtime.RexRuntime
import com.rexaps.rexcoder.runtime.SshSession
import com.rexaps.rexcoder.runtime.parseSshCommand
import kotlinx.coroutines.*
import java.io.File

fun RexCoderState.__split_refreshWorkspace_1() {
        RexRuntime.ensureLayout()
        val oldOpen = docs.keys.toSet()
        root = WorkspaceManager.readTree()
        expanded.clear()
        expanded.add(root.path)
        oldOpen.forEach { p -> if (!File(p).exists()) docs.remove(p) }
        statusMessage = "Workspace refreshed"
    }

fun RexCoderState.__split_openFile_2(node: FileNode, groupId: Int = focusedGroupId) {
        if (node.isDir) { toggle(node.path); return }
        val fresh = runCatching { File(node.path).readText(Charsets.UTF_8) }.getOrDefault(node.content)
        docs[node.path]?.let { if (!it.modified) it.reload(fresh) }
        docs.getOrPut(node.path) { DocState(node.path, node.name, fresh) }
        val g = groups.firstOrNull { it.id == groupId } ?: groups.first()
        if (node.path !in g.tabs) g.tabs.add(node.path)
        g.active = node.path
        focusedGroupId = g.id
        statusMessage = "Opened ${node.name}"
    }

fun RexCoderState.__split_openAt_3(path: String, line: Int) {
        val node = root.flatFiles().firstOrNull { it.path == path } ?: return
        openFile(node)
        val d = docs[path] ?: return
        var offset = 0
        repeat((line - 1).coerceAtLeast(0)) {
            val i = d.value.text.indexOf('\n', offset)
            if (i < 0) return@repeat
            offset = i + 1
        }
        d.value = d.value.copy(selection = TextRange(offset.coerceIn(0, d.value.text.length)))
    }

fun RexCoderState.__split_closeTab_6(g: EditorGroup, path: String) {
        val i = g.tabs.indexOf(path); if (i < 0) return
        g.tabs.removeAt(i)
        if (g.active == path) g.active = g.tabs.getOrNull(minOf(i, g.tabs.lastIndex))
        if (g.tabs.isEmpty() && groups.size > 1) removeGroup(g.id)
    }

internal fun RexCoderState.__split_saveDoc_7(doc: DocState) {
        runCatching { WorkspaceManager.write(File(doc.path), doc.value.text); doc.markSaved(); statusMessage = "Saved ${doc.name}" }
            .onFailure { statusMessage = "Save failed: ${it.message ?: "unknown error"}" }
    }

fun RexCoderState.__split_insertAtCursor_8(t: String) {
        val d = focusedDoc ?: return
        val v = d.value; val sel = v.selection
        d.value = TextFieldValue(v.text.replaceRange(sel.min, sel.max, t), TextRange(sel.min + t.length))
    }

fun RexCoderState.__split_renameNode_9(node: FileNode, newName: String) {
        if (newName.isBlank()) return
        val target = File(node.path).parentFile?.resolve(newName) ?: return
        runCatching { File(node.path).renameTo(target) }
        refreshWorkspace()
    }

/**
     * Masukkan output mentah dari SSH (boleh berisi ANSI escape).
     * Menyambung baris yang belum selesai antar chunk, menangani CRLF / CR, dan
     * menahan escape sequence yang terpotong. Warna dirender di UI oleh AnsiText.
     */
    fun RexCoderState.__split_appendTerminalOutput_10(chunk: String) {
        val data = termPending + chunk
        termPending = ""
        val cut = AnsiText.incompleteEscapeStart(data)
        val usable = if (cut >= 0) { termPending = data.substring(cut); data.substring(0, cut) } else data
        if (usable.isEmpty()) return

        val sb = StringBuilder()
        if (termPartial && terminalLines.isNotEmpty()) sb.append(terminalLines.removeAt(terminalLines.lastIndex))

        var i = 0
        while (i < usable.length) {
            val c = usable[i]
            when (c) {
                '\r' -> {
                    val next = usable.getOrNull(i + 1)
                    if (next != null && next != '\n') sb.setLength(0) // CR murni: timpa baris
                }
                '\n' -> { terminalLines.add(sb.toString()); sb.setLength(0) }
                else -> sb.append(c)
            }
            i++
        }
        if (sb.isNotEmpty()) { terminalLines.add(sb.toString()); termPartial = true } else termPartial = false
        trimTerminal()
    }

/** Dipanggil UI saat ukuran panel/font terminal berubah. */
    fun RexCoderState.__split_resizeTerminal_11(cols: Int, rows: Int) {
        val c = cols.coerceIn(20, 300)
        val r = rows.coerceIn(5, 120)
        if (c == terminalCols && r == terminalRows) return
        terminalCols = c
        terminalRows = r
        sshSession?.resize(c, r)
    }

fun RexCoderState.__split_submitTerminal_12() {
        val line = terminalInput.trim()

        if (sshConnected) {
            terminalInput = ""
            // Remote PTY sudah echo sendiri, jadi tidak ada echo lokal (hindari baris dobel).
            if (line == "exit" || line == "logout") disconnectSsh()
            else sshSession?.send((line + "\n").toByteArray(Charsets.UTF_8))
            return
        }

        if (line.isBlank()) return
        terminalInput = ""
        addLine("$ $line")

        if (line == "ssh" || line.startsWith("ssh ")) {
            val target = parseSshCommand(line)
            if (target == null) {
                addLine("error: Format SSH salah. Contoh: ssh -p 8022 user@192.168.0.101")
            } else {
                sshCommand = line
                sshPassword = ""
                sshLoginVisible = true
            }
            return
        }
        scope.launch {
            val result = withContext(Dispatchers.IO) { terminal.execute(line) }
            result.fold(
                { out -> if (out == "\u000C") clearTerminal() else if (out.isNotBlank()) out.lines().forEach { addLine(it) } },
                { e -> addLine("error: ${e.message ?: "command failed"}") }
            )
            terminalCwd = terminal.cwd.absolutePath
            refreshWorkspace()
        }
    }

fun RexCoderState.__split_connectSsh_13(command: String, password: String) {
        val target = parseSshCommand(command) ?: run {
            addLine("error: Format SSH salah")
            return
        }
        if (sshConnected) disconnectSsh()
        sshLoginVisible = false
        termPending = ""
        addLine("Connecting to ${target.user}@${target.host}:${target.port} ...")
        val session = SshSession()
        sshSession = session
        scope.launch {
            try {
                session.connect(target, password, terminalCols, terminalRows,
                    onText = { text ->
                        scope.launch(Dispatchers.Main.immediate) { appendTerminalOutput(text) }
                    },
                    onClosed = {
                        scope.launch(Dispatchers.Main.immediate) {
                            if (sshSession === session) {
                                sshConnected = false; sshTarget = null; sshSession = null
                                addLine("[SSH] Connection closed")
                            }
                        }
                    }
                )
                sshTarget = target
                sshConnected = true
                addLine("[SSH] Connected to ${target.user}@${target.host}:${target.port}")
                if (sshAutoCd) autoCdToWorkspace(session)
            } catch (e: Exception) {
                session.close()
                if (sshSession === session) sshSession = null
                addLine("error: SSH ${e.message ?: "connection failed"}")
            }
        }
    }
