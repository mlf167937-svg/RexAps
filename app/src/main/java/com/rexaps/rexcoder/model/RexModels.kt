package com.rexaps.rexcoder.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.rexaps.rexcoder.storage.WorkspaceManager
import com.rexaps.rexcoder.runtime.RexRuntime
import com.rexaps.rexcoder.runtime.RexTerminal
import com.rexaps.rexcoder.runtime.SshSession
import com.rexaps.rexcoder.runtime.SshTarget
import com.rexaps.rexcoder.runtime.parseSshCommand
import kotlinx.coroutines.*
import java.io.File

// Real filesystem tree. path is the absolute filesystem path.
data class FileNode(
    val path: String,
    val name: String,
    val isDir: Boolean,
    val children: List<FileNode> = emptyList(),
    val content: String = ""
)

fun FileNode.flatFiles(): List<FileNode> = if (!isDir) listOf(this) else children.flatMap { it.flatFiles() }

enum class SideView(val title: String, val icon: ImageVector) {
    Explorer("Explorer", Icons.Outlined.Folder),
    Search("Search", Icons.Outlined.Search),
    Git("Source Control", Icons.Outlined.AccountTree),
    Extensions("Extensions", Icons.Outlined.Extension)
}

@Stable
class DocState(val path: String, val name: String, text: String) {
    var value by mutableStateOf(TextFieldValue(text))
    private var saved by mutableStateOf(text)
    val modified: Boolean get() = value.text != saved
    fun markSaved() { saved = value.text }
    fun reload(text: String) { value = TextFieldValue(text); saved = text }
}

const val DEFAULT_EDITOR_FONT = 13
const val MIN_EDITOR_FONT = 6
const val MAX_EDITOR_FONT = 32

/** Smallest share (of the total weight) an editor group may shrink to while dragging a divider. */
const val MIN_GROUP_SHARE = 0.18f

@Stable
class EditorGroup(val id: Int, fontSize: Int = DEFAULT_EDITOR_FONT) {
    val tabs = mutableStateListOf<String>()
    var active by mutableStateOf<String?>(null)

    /** Per-editor font size (E1, E2, E3 each keep their own). */
    var fontSize by mutableIntStateOf(fontSize)

    /** Relative size of this editor inside the editor area; changed by dragging dividers. */
    var weight by mutableFloatStateOf(1f)
}

data class SearchHit(val path: String, val name: String, val line: Int, val text: String)

@Stable
class RexCoderState(initialRoot: FileNode = WorkspaceManager.readTree()) {
    private val terminal = RexTerminal()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val terminalLines = mutableStateListOf<String>("RexCoder Terminal", "Workspace: ${WorkspaceManager.DISPLAY_PATH}", "Type \"help\" for commands.")
    var terminalInput by mutableStateOf("")
    var terminalCwd by mutableStateOf(WorkspaceManager.DISPLAY_PATH)
    var root by mutableStateOf(initialRoot)
    val docs = mutableStateMapOf<String, DocState>()
    val groups = mutableStateListOf(EditorGroup(0))
    val expanded = mutableStateListOf<String>()
    var focusedGroupId by mutableIntStateOf(0)
    var view by mutableStateOf(SideView.Explorer)
    var sidebarVisible by mutableStateOf(true)
    var panelVisible by mutableStateOf(false)
    var paletteVisible by mutableStateOf(false)
    var statusMessage by mutableStateOf("Ready")

    // Resizable chrome (dp). Dragged with the dividers in the layouts.
    var sidebarWidth by mutableFloatStateOf(260f)
    var panelHeight by mutableFloatStateOf(190f)

    var sshLoginVisible by mutableStateOf(false)
    var sshCommand by mutableStateOf("")
    var sshPassword by mutableStateOf("")
    var sshConnected by mutableStateOf(false)
    var sshTarget by mutableStateOf<SshTarget?>(null)

    /** When true, a successful SSH login immediately runs `cd` into the RexCoder workspace path. */
    var sshAutoCd by mutableStateOf(true)
    private var sshSession: SshSession? = null
    private var nextGroupId = 1

    init {
        RexRuntime.ensureLayout()
        terminalLines.clear()
        terminalLines.add("RexCoder Terminal")
        terminalLines.add("$ termux-setup-storage")
        terminalLines.add("[RexCoder] storage workspace ready")
        terminalLines.add("$ cd ${WorkspaceManager.DISPLAY_PATH}")
        terminalLines.add(WorkspaceManager.DISPLAY_PATH)
        terminalLines.add("Type \"help\" for commands. Type \"ssh ...\" to open SSH login.")
    }

    val focusedGroup: EditorGroup get() = groups.firstOrNull { it.id == focusedGroupId } ?: groups.first()
    val focusedDoc: DocState? get() = focusedGroup.active?.let { docs[it] }

    fun refreshWorkspace() {
        RexRuntime.ensureLayout()
        val oldOpen = docs.keys.toSet()
        root = WorkspaceManager.readTree()
        expanded.clear()
        expanded.add(root.path)
        oldOpen.forEach { p -> if (!File(p).exists()) docs.remove(p) }
        statusMessage = "Workspace refreshed"
    }

    fun toggle(path: String) { if (!expanded.remove(path)) expanded.add(path) }

    fun openFile(node: FileNode, groupId: Int = focusedGroupId) {
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

    fun openToSide(node: FileNode) {
        val g = if (groups.size < 3) addGroup() else groups.last()
        openFile(node, g.id)
    }

    fun openAt(path: String, line: Int) {
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
    fun addGroup(): EditorGroup = EditorGroup(nextGroupId++, focusedGroup.fontSize).also {
        groups.add(it)
        focusedGroupId = it.id
        equalizeGroups()
    }

    fun splitEditor() { if (groups.size < 3) { val src = focusedGroup.active; val g = addGroup(); if (src != null) { g.tabs.add(src); g.active = src } } }

    fun removeGroup(id: Int) {
        if (groups.size > 1) {
            groups.removeAll { it.id == id }
            if (focusedGroupId == id) focusedGroupId = groups.first().id
            equalizeGroups()
        }
    }

    fun setLayout(n: Int) { while (groups.size < n) splitEditor(); while (groups.size > n) removeGroup(groups.last().id) }

    /** Reset every editor to the same size. */
    fun equalizeGroups() { groups.forEach { it.weight = 1f } }

    /**
     * Drag the divider that sits between groups[leftIndex] and groups[leftIndex + 1].
     * [deltaPx] is the finger movement along the split axis, [totalPx] the pixels available to all groups.
     */
    fun resizeGroups(leftIndex: Int, deltaPx: Float, totalPx: Float) {
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

    fun setFontSize(g: EditorGroup, px: Int) {
        g.fontSize = px.coerceIn(MIN_EDITOR_FONT, MAX_EDITOR_FONT)
        statusMessage = "E${groups.indexOf(g) + 1} font ${g.fontSize}px"
    }

    fun adjustFocusedFont(delta: Int) = setFontSize(focusedGroup, focusedGroup.fontSize + delta)

    fun closeTab(g: EditorGroup, path: String) {
        val i = g.tabs.indexOf(path); if (i < 0) return
        g.tabs.removeAt(i)
        if (g.active == path) g.active = g.tabs.getOrNull(minOf(i, g.tabs.lastIndex))
        if (g.tabs.isEmpty() && groups.size > 1) removeGroup(g.id)
    }
    fun closeAll() { setLayout(1); groups.first().apply { tabs.clear(); active = null } }

    fun save() {
        focusedDoc?.let { saveDoc(it) }
    }
    fun saveAll() { docs.values.filter { it.modified }.forEach(::saveDoc) }
    private fun saveDoc(doc: DocState) {
        runCatching { WorkspaceManager.write(File(doc.path), doc.value.text); doc.markSaved(); statusMessage = "Saved ${doc.name}" }
            .onFailure { statusMessage = "Save failed: ${it.message ?: "unknown error"}" }
    }
    fun modifiedDocs(): List<DocState> = docs.values.filter { it.modified }

    fun insertAtCursor(t: String) {
        val d = focusedDoc ?: return
        val v = d.value; val sel = v.selection
        d.value = TextFieldValue(v.text.replaceRange(sel.min, sel.max, t), TextRange(sel.min + t.length))
    }

    fun createFile(relativePath: String) { WorkspaceManager.createFile(relativePath); refreshWorkspace(); statusMessage = "Created $relativePath" }
    fun createFolder(relativePath: String) { WorkspaceManager.createDirectory(relativePath); refreshWorkspace(); statusMessage = "Created $relativePath" }
    fun deleteNode(node: FileNode) { runCatching { File(node.path).deleteRecursively() }; docs.remove(node.path); refreshWorkspace() }
    fun renameNode(node: FileNode, newName: String) {
        if (newName.isBlank()) return
        val target = File(node.path).parentFile?.resolve(newName) ?: return
        runCatching { File(node.path).renameTo(target) }
        refreshWorkspace()
    }

    fun submitTerminal() {
        val line = terminalInput.trim()
        if (line.isBlank()) return
        terminalInput = ""
        terminalLines.add(if (sshConnected) "remote$ $line" else "$ $line")
        if (sshConnected) {
            if (line == "exit" || line == "logout") {
                disconnectSsh()
            } else {
                sshSession?.send((line + "\n").toByteArray(Charsets.UTF_8))
            }
            return
        }
        if (line == "ssh" || line.startsWith("ssh ")) {
            val target = parseSshCommand(line)
            if (target == null) {
                terminalLines.add("error: Format SSH salah. Contoh: ssh -p 8022 user@192.168.0.101")
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
                { out -> if (out == "\u000C") terminalLines.clear() else if (out.isNotBlank()) terminalLines.addAll(out.lines()) },
                { e -> terminalLines.add("error: ${e.message ?: "command failed"}") }
            )
            terminalCwd = terminal.cwd.absolutePath
            refreshWorkspace()
        }
    }

    fun connectSsh(command: String, password: String) {
        val target = parseSshCommand(command) ?: run {
            terminalLines.add("error: Format SSH salah")
            return
        }
        if (sshConnected) disconnectSsh()
        sshLoginVisible = false
        terminalLines.add("Connecting to ${target.user}@${target.host}:${target.port} ...")
        val session = SshSession()
        sshSession = session
        scope.launch {
            try {
                session.connect(target, password, 120, 36,
                    onText = { text ->
                        scope.launch(Dispatchers.Main.immediate) { terminalLines.addAll(text.replace("\r", "").split('\n')) }
                    },
                    onClosed = {
                        scope.launch(Dispatchers.Main.immediate) { if (sshSession === session) { sshConnected = false; sshTarget = null; sshSession = null; terminalLines.add("[SSH] Connection closed") } }
                    }
                )
                sshTarget = target
                sshConnected = true
                terminalLines.add("[SSH] Connected to ${target.user}@${target.host}:${target.port}")
                if (sshAutoCd) autoCdToWorkspace(session)
            } catch (e: Exception) {
                session.close()
                if (sshSession === session) sshSession = null
                terminalLines.add("error: SSH ${e.message ?: "connection failed"}")
            }
        }
    }

    /** Equivalent of typing `cd /storage/emulated/0/Download/RexAps/RexCoder` right after login. */
    private fun autoCdToWorkspace(session: SshSession) {
        val path = WorkspaceManager.DISPLAY_PATH.trimEnd('/')
        terminalLines.add("[SSH] Auto cd → $path")
        session.send("cd '$path'\n".toByteArray(Charsets.UTF_8))
    }

    fun openSshLogin() {
        sshCommand = if (sshCommand.isBlank()) "ssh -p 22 user@host" else sshCommand
        sshPassword = ""
        sshLoginVisible = true
    }

    fun disconnectSsh() {
        sshSession?.close()
        sshSession = null
        sshConnected = false
        sshTarget = null
        terminalLines.add("[SSH] Disconnected")
    }

    fun search(q: String): List<SearchHit> {
        if (q.length < 2) return emptyList()
        return root.flatFiles().asSequence().flatMap { f ->
            val text = docs[f.path]?.value?.text ?: f.content
            text.lineSequence().mapIndexedNotNull { i, l -> if (l.contains(q, true)) SearchHit(f.path, f.name, i + 1, l.trim()) else null }
        }.take(200).toList()
    }
}

@Composable
fun rememberRexCoderState(): RexCoderState = remember { RexCoderState().also { it.expanded.add(it.root.path) } }
