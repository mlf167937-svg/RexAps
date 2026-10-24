package com.rexaps.rexcoder.model

import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import com.rexaps.rexcoder.storage.WorkspaceManager
import com.rexaps.rexcoder.runtime.AnsiText
import com.rexaps.rexcoder.runtime.SshSession
import com.rexaps.rexcoder.runtime.parseSshCommand
import kotlinx.coroutines.*

// ───────────── Terminal output helpers ─────────────

private fun RexCoderState.trimTerminal() {
    val extra = terminalLines.size - MAX_TERMINAL_LINES
    if (extra > 0) repeat(extra) { terminalLines.removeAt(0) }
}

/** Tambah satu baris utuh (pesan lokal). Baris SSH yang belum selesai dibiarkan apa adanya. */
internal fun RexCoderState.addLine(text: String) {
    termPartial = false
    terminalLines.add(text)
    trimTerminal()
}

/**
 * Masukkan output mentah dari SSH (boleh berisi ANSI escape).
 * Menyambung baris yang belum selesai antar chunk, menangani CRLF / CR, dan
 * menahan escape sequence yang terpotong. Warna dirender di UI oleh AnsiText.
 */
internal fun RexCoderState.appendTerminalOutputImpl(chunk: String) {
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
internal fun RexCoderState.resizeTerminalImpl(cols: Int, rows: Int) {
    val c = cols.coerceIn(20, 300)
    val r = rows.coerceIn(5, 120)
    if (c == terminalCols && r == terminalRows) return
    terminalCols = c
    terminalRows = r
    sshSession?.resize(c, r)
}

internal fun RexCoderState.submitTerminalImpl() {
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

internal fun RexCoderState.connectSshImpl(command: String, password: String) {
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

/** Equivalent of typing `cd /storage/emulated/0/Download/RexAps/RexCoder` right after login. */
private fun RexCoderState.autoCdToWorkspace(session: SshSession) {
    val path = WorkspaceManager.DISPLAY_PATH.trimEnd('/')
    addLine("[SSH] Auto cd → $path")
    session.send("cd '$path'\n".toByteArray(Charsets.UTF_8))
}
