package com.rexaps.rexcoder.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.TextFieldValue
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

internal fun RexCoderState.renameNodeImpl(node: FileNode, newName: String) {
    if (newName.isBlank()) return
    val target = File(node.path).parentFile?.resolve(newName) ?: return
    runCatching { File(node.path).renameTo(target) }
    refreshWorkspace()
}
