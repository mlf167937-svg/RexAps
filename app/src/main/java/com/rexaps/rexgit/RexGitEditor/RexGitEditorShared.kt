@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector

// ============================================================
// Editor
// ============================================================

internal val EditorKeys = listOf(
    "Tab" to "    ",
    "{" to "{", "}" to "}",
    "(" to "(", ")" to ")",
    "[" to "[", "]" to "]",
    "<" to "<", ">" to ">",
    ";" to ";", ":" to ":",
    "\"" to "\"", "'" to "'",
    "=" to "=", "/" to "/", "#" to "#"
)

internal const val EDITOR_MIN_FONT = 2

internal const val EDITOR_MAX_FONT = 38

internal const val EDITOR_DEFAULT_FONT = 14

/** How long the px picker waits (after you stop) before the text is re-laid out. */
internal const val FONT_APPLY_DELAY_MS = 350L

// ---------- parts used by both layouts ----------

internal class EditorAction(
    val label: String,
    val icon: ImageVector?,
    val onClick: () -> Unit
)
