package com.rexaps.rexcoder.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.TextFieldValue
import com.rexaps.rexcoder.storage.WorkspaceManager
import com.rexaps.rexcoder.runtime.RexRuntime
import com.rexaps.rexcoder.runtime.RexTerminal
import com.rexaps.rexcoder.runtime.SshSession
import com.rexaps.rexcoder.runtime.SshTarget
import kotlinx.coroutines.*

// Real filesystem tree. path is the absolute filesystem path.
data class FileNode(
    val path: String,
    val name: String,
    val isDir: Boolean,
    val children: List<FileNode> = emptyList(),
    val content: String = ""
)

fun FileNode.flatFiles(): List<FileNode> = this.__splitImpl_flatFiles()

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

// ───────────── Ukuran font ─────────────
const val DEFAULT_EDITOR_FONT = 13
const val MIN_EDITOR_FONT = 2
const val MAX_EDITOR_FONT = 22

const val DEFAULT_TREE_FONT = 13
const val MIN_TREE_FONT = 4
const val MAX_TREE_FONT = 14

const val DEFAULT_TERMINAL_FONT = 12
const val MIN_TERMINAL_FONT = 6
const val MAX_TERMINAL_FONT = 20

/** Maksimum baris riwayat terminal yang disimpan. */
const val MAX_TERMINAL_LINES = 3000

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
        internal set
    var terminalRows = 24
        internal set

    var sshLoginVisible by mutableStateOf(false)
    var sshCommand by mutableStateOf("")
    var sshPassword by mutableStateOf("")
    var sshConnected by mutableStateOf(false)
    var sshTarget by mutableStateOf<SshTarget?>(null)

    /** When true, a successful SSH login immediately runs `cd` into the RexCoder workspace path. */
    var sshAutoCd by mutableStateOf(true)
    internal var sshSession: SshSession? = null
    internal var nextGroupId = 1

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

    fun refreshWorkspace() {
        return this.__splitImpl_refreshWorkspace()
    }

    fun toggle(path: String) { if (!expanded.remove(path)) expanded.add(path) }

    fun openFile(node: FileNode, groupId: Int = focusedGroupId) {
        return this.__splitImpl_openFile(node, groupId)
    }

    fun openToSide(node: FileNode) {
        return this.__splitImpl_openToSide(node)
    }

    fun openAt(path: String, line: Int) {
        return this.__splitImpl_openAt(path, line)
    }

    // ───────────── Editor groups: add / remove / resize / font ─────────────

    /** New groups inherit the focused editor's font size; all groups are re-balanced to equal width. */
    fun addGroup(): EditorGroup = this.__splitImpl_addGroup()

    fun splitEditor() {
        return this.__splitImpl_splitEditor()
    }

    fun removeGroup(id: Int) {
        return this.__splitImpl_removeGroup(id)
    }

    fun setLayout(n: Int) {
        return this.__splitImpl_setLayout(n)
    }

    /** Reset every editor to the same size. */
    fun equalizeGroups() { groups.forEach { it.weight = 1f } }

    /**
     * Drag the divider that sits between groups[leftIndex] and groups[leftIndex + 1].
     * [deltaPx] is the finger movement along the split axis, [totalPx] the pixels available to all groups.
     */
    fun resizeGroups(leftIndex: Int, deltaPx: Float, totalPx: Float) {
        return this.__splitImpl_resizeGroups(leftIndex, deltaPx, totalPx)
    }

    fun setFontSize(g: EditorGroup, px: Int) {
        return this.__splitImpl_setFontSize(g, px)
    }

    fun adjustFocusedFont(delta: Int) = setFontSize(focusedGroup, focusedGroup.fontSize + delta)

    fun setTreeFont(px: Int) {
        return this.__splitImpl_setTreeFont(px)
    }

    fun setTerminalFont(px: Int) {
        return this.__splitImpl_setTerminalFont(px)
    }

    fun closeTab(g: EditorGroup, path: String) {
        return this.__splitImpl_closeTab(g, path)
    }
    fun closeAll() {
        return this.__splitImpl_closeAll()
    }

    fun save() {
        focusedDoc?.let { saveDoc(it) }
    }
    fun saveAll() { docs.values.filter { it.modified }.forEach(::saveDoc) }
    internal fun saveDoc(doc: DocState) {
        return this.__splitImpl_saveDoc(doc)
    }
    fun modifiedDocs(): List<DocState> = docs.values.filter { it.modified }

    fun insertAtCursor(t: String) {
        return this.__splitImpl_insertAtCursor(t)
    }

    fun createFile(relativePath: String) {
        return this.__splitImpl_createFile(relativePath)
    }
    fun createFolder(relativePath: String) {
        return this.__splitImpl_createFolder(relativePath)
    }
    fun deleteNode(node: FileNode) {
        return this.__splitImpl_deleteNode(node)
    }
    fun renameNode(node: FileNode, newName: String) {
        return this.__splitImpl_renameNode(node, newName)
    }

    // ───────────── Terminal output helpers ─────────────

    internal fun trimTerminal() {
        return this.__splitImpl_trimTerminal()
    }

    /** Tambah satu baris utuh (pesan lokal). Baris SSH yang belum selesai dibiarkan apa adanya. */
    internal fun addLine(text: String) {
        return this.__splitImpl_addLine(text)
    }

    fun clearTerminal() {
        return this.__splitImpl_clearTerminal()
    }

    /**
     * Masukkan output mentah dari SSH (boleh berisi ANSI escape).
     * Menyambung baris yang belum selesai antar chunk, menangani CRLF / CR, dan
     * menahan escape sequence yang terpotong. Warna dirender di UI oleh AnsiText.
     */
    fun appendTerminalOutput(chunk: String) {
        return this.__splitImpl_appendTerminalOutput(chunk)
    }

    /** Dipanggil UI saat ukuran panel/font terminal berubah. */
    fun resizeTerminal(cols: Int, rows: Int) {
        return this.__splitImpl_resizeTerminal(cols, rows)
    }

    /** Ctrl+C ke sesi SSH. */
    fun sendInterrupt() { sshSession?.send(byteArrayOf(3)) }

    fun submitTerminal() {
        return this.__splitImpl_submitTerminal()
    }

    fun connectSsh(command: String, password: String) {
        return this.__splitImpl_connectSsh(command, password)
    }

    /** Equivalent of typing `cd /storage/emulated/0/Download/RexAps/RexCoder` right after login. */
    internal fun autoCdToWorkspace(session: SshSession) {
        return this.__splitImpl_autoCdToWorkspace(session)
    }

    fun openSshLogin() {
        return this.__splitImpl_openSshLogin()
    }

    fun disconnectSsh() {
        return this.__splitImpl_disconnectSsh()
    }

    fun search(q: String): List<SearchHit> {
        return this.__splitImpl_search(q)
    }
}

@Composable
fun rememberRexCoderState(): RexCoderState = __splitImpl_rememberRexCoderState()
