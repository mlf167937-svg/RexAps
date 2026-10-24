package com.rexaps.rexcoder.model

import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.rexaps.rexcoder.runtime.AnsiText
import com.rexaps.rexcoder.runtime.SshSession
import com.rexaps.rexcoder.runtime.parseSshCommand
import com.rexaps.rexcoder.storage.WorkspaceManager
import java.io.File
import kotlinx.coroutines.*

internal fun RexCoderState.__splitImpl_connectSsh(command: String, password: String) {
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

/**
     * Masukkan output mentah dari SSH (boleh berisi ANSI escape).
     * Menyambung baris yang belum selesai antar chunk, menangani CRLF / CR, dan
     * menahan escape sequence yang terpotong. Warna dirender di UI oleh AnsiText.
     */
    internal fun RexCoderState.__splitImpl_appendTerminalOutput(chunk: String) {
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

/**
     * Drag the divider that sits between groups[leftIndex] and groups[leftIndex + 1].
     * [deltaPx] is the finger movement along the split axis, [totalPx] the pixels available to all groups.
     */
    internal fun RexCoderState.__splitImpl_resizeGroups(leftIndex: Int, deltaPx: Float, totalPx: Float) {
        val a = groups.getOrNull(leftIndex) ?: return
        val b = groups.getOrNull(leftIndex + 1) ?: return
        if (totalPx <= 0f) return
        val total = groups.sumOf { it.weight.toDouble() }.toFloat()
        val min = total * MIN_GROUP_SHARE
        val pair = a.weight + b.weight
        if (pair < min * 2f) return
        val next = (a.weight + deltaPx / totalPx * total).coerceIn(min, pair - min)
        a.weight = next
        b.weight = pair - next
    }

internal fun RexCoderState.__splitImpl_openAt(path: String, line: Int) {
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

internal fun RexCoderState.__splitImpl_search(q: String): List<SearchHit> {
        if (q.length < 2) return emptyList()
        return root.flatFiles().asSequence().flatMap { f ->
            val text = docs[f.path]?.value?.text ?: f.content
            text.lineSequence().mapIndexedNotNull { i, l -> if (l.contains(q, true)) SearchHit(f.path, f.name, i + 1, l.trim()) else null }
        }.take(200).toList()
    }

/** Dipanggil UI saat ukuran panel/font terminal berubah. */
    internal fun RexCoderState.__splitImpl_resizeTerminal(cols: Int, rows: Int) {
        val c = cols.coerceIn(20, 300)
        val r = rows.coerceIn(5, 120)
        if (c == terminalCols && r == terminalRows) return
        terminalCols = c
        terminalRows = r
        sshSession?.resize(c, r)
    }

internal fun RexCoderState.__splitImpl_renameNode(node: FileNode, newName: String) {
        if (newName.isBlank()) return
        val target = File(node.path).parentFile?.resolve(newName) ?: return
        runCatching { File(node.path).renameTo(target) }
        refreshWorkspace()
    }

internal fun RexCoderState.__splitImpl_insertAtCursor(t: String) {
        val d = focusedDoc ?: return
        val v = d.value; val sel = v.selection
        d.value = TextFieldValue(v.text.replaceRange(sel.min, sel.max, t), TextRange(sel.min + t.length))
    }

internal fun RexCoderState.__splitImpl_disconnectSsh() {
        sshSession?.close()
        sshSession = null
        sshConnected = false
        sshTarget = null
        termPending = ""
        addLine("[SSH] Disconnected")
    }

/** Equivalent of typing `cd /storage/emulated/0/Download/RexAps/RexCoder` right after login. */
    internal fun RexCoderState.__splitImpl_autoCdToWorkspace(session: SshSession) {
        val path = WorkspaceManager.DISPLAY_PATH.trimEnd('/')
        addLine("[SSH] Auto cd → $path")
        session.send("cd '$path'\n".toByteArray(Charsets.UTF_8))
    }

internal fun RexCoderState.__splitImpl_openSshLogin() {
        sshCommand = if (sshCommand.isBlank()) "ssh -p 22 user@host" else sshCommand
        sshPassword = ""
        sshLoginVisible = true
    }

internal fun RexCoderState.__splitImpl_setFontSize(g: EditorGroup, px: Int) {
        g.fontSize = px.coerceIn(MIN_EDITOR_FONT, MAX_EDITOR_FONT)
        statusMessage = "E${groups.indexOf(g) + 1} font ${g.fontSize}px"
    }

internal fun RexCoderState.__splitImpl_setTerminalFont(px: Int) {
        terminalFontSize = px.coerceIn(MIN_TERMINAL_FONT, MAX_TERMINAL_FONT)
        statusMessage = "Terminal font ${terminalFontSize}px"
    }

/** New groups inherit the focused editor's font size; all groups are re-balanced to equal width. */
    internal fun RexCoderState.__splitImpl_addGroup(): EditorGroup = EditorGroup(nextGroupId++, focusedGroup.fontSize).also {
        groups.add(it)
        focusedGroupId = it.id
        equalizeGroups()
    }

internal fun RexCoderState.__splitImpl_trimTerminal() {
        val extra = terminalLines.size - MAX_TERMINAL_LINES
        if (extra > 0) repeat(extra) { terminalLines.removeAt(0) }
    }

internal fun RexCoderState.__splitImpl_splitEditor() { if (groups.size < 3) { val src = focusedGroup.active; val g = addGroup(); if (src != null) { g.tabs.add(src); g.active = src } } }

internal fun RexCoderState.__splitImpl_setTreeFont(px: Int) {
        treeFontSize = px.coerceIn(MIN_TREE_FONT, MAX_TREE_FONT)
        statusMessage = "Tree font ${treeFontSize}px"
    }

internal fun RexCoderState.__splitImpl_openToSide(node: FileNode) {
        val g = if (groups.size < 3) addGroup() else groups.last()
        openFile(node, g.id)
    }

internal fun RexCoderState.__splitImpl_setLayout(n: Int) { while (groups.size < n) splitEditor(); while (groups.size > n) removeGroup(groups.last().id) }

/** Tambah satu baris utuh (pesan lokal). Baris SSH yang belum selesai dibiarkan apa adanya. */
    internal fun RexCoderState.__splitImpl_addLine(text: String) {
        termPartial = false
        terminalLines.add(text)
        trimTerminal()
    }

internal fun RexCoderState.__splitImpl_clearTerminal() {
        terminalLines.clear()
        termPartial = false
        termPending = ""
    }

internal fun RexCoderState.__splitImpl_closeAll() { setLayout(1); groups.first().apply { tabs.clear(); active = null } }
