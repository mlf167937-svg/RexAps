package com.rexaps.rexcoder.model

import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.TextFieldValue
import com.rexaps.rexcoder.storage.WorkspaceManager
import com.rexaps.rexcoder.runtime.AnsiText
import com.rexaps.rexcoder.runtime.RexRuntime
import com.rexaps.rexcoder.runtime.RexTerminal
import com.rexaps.rexcoder.runtime.SshSession
import com.rexaps.rexcoder.runtime.SshTarget
import kotlinx.coroutines.*
import java.io.File

@Stable
class DocState(val path: String, val name: String, text: String) {
    var value by mutableStateOf(TextFieldValue(text))
    private var saved by mutableStateOf(text)
    val modified: Boolean get() = value.text != saved
    fun markSaved() { saved = value.text }
    fun reload(text: String) { value = TextFieldValue(text); saved = text }
}

@Stable
class RexCoderState(initialRoot: FileNode = WorkspaceManager.readTree()) {
    internal val terminal = RexTerminal()
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
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

    /** Ukuran font tree project (4..14, default 13). */
    var treeFontSize by mutableIntStateOf(DEFAULT_TREE_FONT)

    /** Ukuran font terminal (6..20, default 12). */
    var terminalFontSize by mutableIntStateOf(DEFAULT_TERMINAL_FONT)

    /** Ukuran terminal dalam karakter; diisi otomatis dari lebar/tinggi panel, dikirim ke PTY SSH. */
    var terminalCols = 80
        private set
    var terminalRows = 24
        private set

    var sshLoginVisible by mutableStateOf(false)
    var sshCommand by mutableStateOf("")
    var sshPassword by mutableStateOf("")
    var sshConnected by mutableStateOf(false)
    var sshTarget by mutableStateOf<SshTarget?>(null)

    /** When true, a successful SSH login immediately runs `cd` into the RexCoder workspace path. */
    var sshAutoCd by mutableStateOf(true)
    internal var sshSession: SshSession? = null
    private var nextGroupId = 1

    // Stream terminal: sisa escape sequence yang terpotong + status baris terakhir belum selesai.
    internal var termPending = ""
    internal var termPartial = false

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

    fun refreshWorkspace() = this.__split_refreshWorkspace_1()

    fun toggle(path: String) { if (!expanded.remove(path)) expanded.add(path) }

    fun openFile(node: FileNode, groupId: Int = focusedGroupId) = this.__split_openFile_2(node, groupId)

    fun openToSide(node: FileNode) {
        val g = if (groups.size < 3) addGroup() else groups.last()
        openFile(node, g.id)
    }

    fun openAt(path: String, line: Int) = this.__split_openAt_3(path, line)

    // ───────────── Editor groups: add / remove / resize / font ─────────────

    /** New groups inherit the focused editor's font size; all groups are re-balanced to equal width. */
    fun addGroup(): EditorGroup = EditorGroup(nextGroupId++, focusedGroup.fontSize).also {
        groups.add(it)
        focusedGroupId = it.id
        equalizeGroups()
    }

    fun splitEditor() { if (groups.size < 3) { val src = focusedGroup.active; val g = addGroup(); if (src != null) { g.tabs.add(src); g.active = src } } }

    fun removeGroup(id: Int) = this.__split_removeGroup_4(id)

    fun setLayout(n: Int) { while (groups.size < n) splitEditor(); while (groups.size > n) removeGroup(groups.last().id) }

    /** Reset every editor to the same size. */
    fun equalizeGroups() { groups.forEach { it.weight = 1f } }

    /**
     * Drag the divider that sits between groups[leftIndex] and groups[leftIndex + 1].
     * [deltaPx] is the finger movement along the split axis, [totalPx] the pixels available to all groups.
     */
    fun resizeGroups(leftIndex: Int, deltaPx: Float, totalPx: Float) = this.__split_resizeGroups_5(leftIndex, deltaPx, totalPx)

    fun setFontSize(g: EditorGroup, px: Int) {
        g.fontSize = px.coerceIn(MIN_EDITOR_FONT, MAX_EDITOR_FONT)
        statusMessage = "E${groups.indexOf(g) + 1} font ${g.fontSize}px"
    }

    fun adjustFocusedFont(delta: Int) = setFontSize(focusedGroup, focusedGroup.fontSize + delta)

    fun setTreeFont(px: Int) {
        treeFontSize = px.coerceIn(MIN_TREE_FONT, MAX_TREE_FONT)
        statusMessage = "Tree font ${treeFontSize}px"
    }

    fun setTerminalFont(px: Int) {
        terminalFontSize = px.coerceIn(MIN_TERMINAL_FONT, MAX_TERMINAL_FONT)
        statusMessage = "Terminal font ${terminalFontSize}px"
    }

    fun closeTab(g: EditorGroup, path: String) = this.__split_closeTab_6(g, path)
    fun closeAll() { setLayout(1); groups.first().apply { tabs.clear(); active = null } }

    fun save() {
        focusedDoc?.let { saveDoc(it) }
    }
    fun saveAll() { docs.values.filter { it.modified }.forEach(::saveDoc) }
    private fun saveDoc(doc: DocState) = this.__split_saveDoc_7(doc)
    fun modifiedDocs(): List<DocState> = docs.values.filter { it.modified }

    fun insertAtCursor(t: String) = this.__split_insertAtCursor_8(t)

    fun createFile(relativePath: String) { WorkspaceManager.createFile(relativePath); refreshWorkspace(); statusMessage = "Created $relativePath" }
    fun createFolder(relativePath: String) { WorkspaceManager.createDirectory(relativePath); refreshWorkspace(); statusMessage = "Created $relativePath" }
    fun deleteNode(node: FileNode) { runCatching { File(node.path).deleteRecursively() }; docs.remove(node.path); refreshWorkspace() }
    fun renameNode(node: FileNode, newName: String) = this.__split_renameNode_9(node, newName)

    // ───────────── Terminal output helpers ─────────────

    internal fun trimTerminal() {
        val extra = terminalLines.size - MAX_TERMINAL_LINES
        if (extra > 0) repeat(extra) { terminalLines.removeAt(0) }
    }

    /** Tambah satu baris utuh (pesan lokal). Baris SSH yang belum selesai dibiarkan apa adanya. */
    internal fun addLine(text: String) {
        termPartial = false
        terminalLines.add(text)
        trimTerminal()
    }

    fun clearTerminal() {
        terminalLines.clear()
        termPartial = false
        termPending = ""
    }

    /**
     * Masukkan output mentah dari SSH (boleh berisi ANSI escape).
     * Menyambung baris yang belum selesai antar chunk, menangani CRLF / CR, dan
     * menahan escape sequence yang terpotong. Warna dirender di UI oleh AnsiText.
     */
    fun appendTerminalOutput(chunk: String) = this.__split_appendTerminalOutput_10(chunk)

    /** Dipanggil UI saat ukuran panel/font terminal berubah. */
    fun resizeTerminal(cols: Int, rows: Int) = this.__split_resizeTerminal_11(cols, rows)

    /** Ctrl+C ke sesi SSH. */
    fun sendInterrupt() { sshSession?.send(byteArrayOf(3)) }

    fun submitTerminal() = this.__split_submitTerminal_12()

    fun connectSsh(command: String, password: String) = this.__split_connectSsh_13(command, password)

    /** Equivalent of typing `cd /storage/emulated/0/Download/RexAps/RexCoder` right after login. */
    internal fun autoCdToWorkspace(session: SshSession) {
        val path = WorkspaceManager.DISPLAY_PATH.trimEnd('/')
        addLine("[SSH] Auto cd → $path")
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
        termPending = ""
        addLine("[SSH] Disconnected")
    }

    fun search(q: String): List<SearchHit> = this.__split_search_14(q)
}

@Composable
fun rememberRexCoderState(): RexCoderState = remember { RexCoderState().also { it.expanded.add(it.root.path) } }
