package com.rexaps.rexcoder.model

import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import com.rexaps.rexcoder.runtime.RexRuntime
import com.rexaps.rexcoder.runtime.parseSshCommand
import com.rexaps.rexcoder.storage.WorkspaceManager
import java.io.File
import kotlinx.coroutines.*

internal fun RexCoderState.__splitImpl_submitTerminal() {
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

internal fun RexCoderState.__splitImpl_refreshWorkspace() {
        RexRuntime.ensureLayout()
        val oldOpen = docs.keys.toSet()
        root = WorkspaceManager.readTree()
        expanded.clear()
        expanded.add(root.path)
        oldOpen.forEach { p -> if (!File(p).exists()) docs.remove(p) }
        statusMessage = "Workspace refreshed"
    }

internal fun RexCoderState.__splitImpl_saveDoc(doc: DocState) {
        runCatching { WorkspaceManager.write(File(doc.path), doc.value.text); doc.markSaved(); statusMessage = "Saved ${doc.name}" }
            .onFailure { statusMessage = "Save failed: ${it.message ?: "unknown error"}" }
    }

internal fun RexCoderState.__splitImpl_removeGroup(id: Int) {
        if (groups.size > 1) {
            groups.removeAll { it.id == id }
            if (focusedGroupId == id) focusedGroupId = groups.first().id
            equalizeGroups()
        }
    }

internal fun RexCoderState.__splitImpl_deleteNode(node: FileNode) { runCatching { File(node.path).deleteRecursively() }; docs.remove(node.path); refreshWorkspace() }
