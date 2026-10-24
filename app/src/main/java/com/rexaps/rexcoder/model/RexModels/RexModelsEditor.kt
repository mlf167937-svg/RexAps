package com.rexaps.rexcoder.model

import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*

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

@Stable
class EditorGroup(val id: Int, fontSize: Int = DEFAULT_EDITOR_FONT) {
    val tabs = mutableStateListOf<String>()
    var active by mutableStateOf<String?>(null)

    /** Per-editor font size (E1, E2, E3 each keep their own). */
    var fontSize by mutableIntStateOf(fontSize)

    /** Relative size of this editor inside the editor area; changed by dragging dividers. */
    var weight by mutableFloatStateOf(1f)
}
