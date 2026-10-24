package com.rexaps.rexcoder.model

import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.rexaps.rexcoder.storage.WorkspaceManager
import com.rexaps.rexcoder.runtime.RexRuntime
import kotlinx.coroutines.*
import java.io.File

internal fun RexCoderState.refreshWorkspaceImpl() {
    RexRuntime.ensureLayout()
    val oldOpen = docs.keys.toSet()
    root = WorkspaceManager.readTree()
    expanded.clear()
    expanded.add(root.path)
    oldOpen.forEach { p -> if (!File(p).exists()) docs.remove(p) }
    statusMessage = "Workspace refreshed"
}

internal fun RexCoderState.openFileImpl(node: FileNode, groupId: Int = focusedGroupId) {
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

internal fun RexCoderState.openAtImpl(path: String, line: Int) {
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

// ───────────── Editor groups: add / remove / resize / font ─────────────

/** New groups inherit the focused editor's font size; all groups are re-balanced to equal width. */
internal fun RexCoderState.addGroupImpl(): EditorGroup = EditorGroup(nextGroupId++, focusedGroup.fontSize).also {
    groups.add(it)
    focusedGroupId = it.id
    equalizeGroups()
}

internal fun RexCoderState.removeGroupImpl(id: Int) {
    if (groups.size > 1) {
        groups.removeAll { it.id == id }
        if (focusedGroupId == id) focusedGroupId = groups.first().id
        equalizeGroups()
    }
}

/**
 * Drag the divider that sits between groups[leftIndex] and groups[leftIndex + 1].
 * [deltaPx] is the finger movement along the split axis, [totalPx] the pixels available to all groups.
 */
internal fun RexCoderState.resizeGroupsImpl(leftIndex: Int, deltaPx: Float, totalPx: Float) {
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

internal fun RexCoderState.closeTabImpl(g: EditorGroup, path: String) {
    val i = g.tabs.indexOf(path); if (i < 0) return
    g.tabs.removeAt(i)
    if (g.active == path) g.active = g.tabs.getOrNull(minOf(i, g.tabs.lastIndex))
    if (g.tabs.isEmpty() && groups.size > 1) removeGroup(g.id)
}

internal fun RexCoderState.saveDoc(doc: DocState) {
    runCatching { WorkspaceManager.write(File(doc.path), doc.value.text); doc.markSaved(); statusMessage = "Saved ${doc.name}" }
        .onFailure { statusMessage = "Save failed: ${it.message ?: "unknown error"}" }
}

internal fun RexCoderState.insertAtCursorImpl(t: String) {
    val d = focusedDoc ?: return
    val v = d.value; val sel = v.selection
    d.value = TextFieldValue(v.text.replaceRange(sel.min, sel.max, t), TextRange(sel.min + t.length))
}

internal fun RexCoderState.searchImpl(q: String): List<SearchHit> {
    if (q.length < 2) return emptyList()
    return root.flatFiles().asSequence().flatMap { f ->
        val text = docs[f.path]?.value?.text ?: f.content
        text.lineSequence().mapIndexedNotNull { i, l -> if (l.contains(q, true)) SearchHit(f.path, f.name, i + 1, l.trim()) else null }
    }.take(200).toList()
}
