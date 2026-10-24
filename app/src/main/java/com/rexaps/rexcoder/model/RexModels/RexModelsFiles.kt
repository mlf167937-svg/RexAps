package com.rexaps.rexcoder.model

import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import com.rexaps.rexcoder.storage.WorkspaceManager
import java.io.File
import kotlinx.coroutines.*

internal fun RexCoderState.__splitImpl_openFile(node: FileNode, groupId: Int = focusedGroupId) {
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

internal fun RexCoderState.__splitImpl_createFolder(relativePath: String) { WorkspaceManager.createDirectory(relativePath); refreshWorkspace(); statusMessage = "Created $relativePath" }

internal fun RexCoderState.__splitImpl_createFile(relativePath: String) { WorkspaceManager.createFile(relativePath); refreshWorkspace(); statusMessage = "Created $relativePath" }

internal fun FileNode.__splitImpl_flatFiles(): List<FileNode> = if (!isDir) listOf(this) else children.flatMap { it.flatFiles() }
