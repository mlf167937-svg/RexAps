
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf


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

/**
 * All editor state lives here (above the portrait / landscape switch) so
 * rotating the phone never loses typing, the cursor, search or the px.
 *
 * px is split in two on purpose:
 *  - [draftPx]  what the slider / A- / A+ show instantly (cheap)
 *  - [fontPx]   what the text field really uses (expensive to re-layout)
 * The editor always opens at the default px. Changing the slider only moves
 * [draftPx]; [fontPx] follows once, after you let go.
 */
@Stable
internal class RexGitEditorState(
    initialText: String,
    initialSelection: TextRange,
    private val appliedFont: MutableState<Int>,
    internal val draftFont: MutableState<Int>,
    private val slidingState: MutableState<Boolean>,
    internal val selectionStart: MutableState<Int>,
    internal val selectionEnd: MutableState<Int>,
    val searchOpen: MutableState<Boolean>,
    val searchQuery: MutableState<String>,
    val searchIndex: MutableState<Int>,
    val goToLineOpen: MutableState<Boolean>,
    val goToLineText: MutableState<String>,
    val focusRequester: FocusRequester,
    internal val onEdit: (String) -> Unit,
    internal val toast: (String) -> Unit
) {
    var textField by mutableStateOf(TextFieldValue(initialText, initialSelection))
        internal set

    /** px used by the text field. */
    val fontPx: Int get() = appliedFont.value

    /** px shown by the controls. */
    val draftPx: Int get() = draftFont.value

    val sliding: Boolean get() = slidingState.value

    /** True while the chosen px has not been applied to the text yet. */
    val fontPending: Boolean get() = draftFont.value != appliedFont.value

    val lines: Int by derivedStateOf { textField.text.count { it == '\n' } + 1 }

    // ---------- px ----------

    fun setDraftPx(px: Int) {
        return this.__splitImpl_setDraftPx(px)
    }

    fun setSliding(value: Boolean) {
        slidingState.value = value
    }

    /** Called by the debounce effect once the picker has been idle. */
    fun applyDraftPx() {
        appliedFont.value = draftFont.value
    }

    // ---------- text ----------

    fun onValueChange(new: TextFieldValue) {
        return this.__splitImpl_onValueChange(new)
    }

    fun insert(snippet: String) {
        return this.__splitImpl_insert(snippet)
    }

    fun selectAll() {
        return this.__splitImpl_selectAll()
    }

    fun message(text: String) = toast(text)

    // ---------- search / go to line ----------

    fun searchMatches(query: String): List<Int> {
        return this.__splitImpl_searchMatches(query)
    }

    fun selectSearchMatch(direction: Int) {
        return this.__splitImpl_selectSearchMatch(direction)
    }

    fun currentLine(): Int {
        return this.__splitImpl_currentLine()
    }

    fun goToLine() {
        return this.__splitImpl_goToLine()
    }
}

@Composable
internal fun rememberRexGitEditorState(
    path: String,
    viewModel: RexGitViewModel
): RexGitEditorState {
        return __splitImpl_rememberRexGitEditorState(path, viewModel)
    }

// ---------- parts used by both layouts ----------

internal class EditorAction(
    val label: String,
    val icon: ImageVector?,
    val onClick: () -> Unit
)

@Composable
internal fun rememberEditorActions(editor: RexGitEditorState): List<EditorAction> {
        return __splitImpl_rememberEditorActions(editor)
    }

@Composable
internal fun EditorToolButton(
    action: EditorAction,
    modifier: Modifier = Modifier
) {
        return __splitImpl_EditorToolButton(action, modifier)
    }

@Composable
internal fun EditorHeader(
    state: RexGitUiState,
    editor: RexGitEditorState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    compact: Boolean = false
) {
        return __splitImpl_EditorHeader(state, editor, onBack, onSave, compact)
    }

/**
 * px controls. They only change the draft; the text is re-laid out once,
 * after the controls have been idle (see rememberRexGitEditorState).
 *
 * [stacked] puts the slider on its own line (narrow side panel in landscape).
 */
@Composable
internal fun EditorFontControls(
    editor: RexGitEditorState,
    modifier: Modifier = Modifier,
    stacked: Boolean = false
) {
        return __splitImpl_EditorFontControls(editor, modifier, stacked)
    }

@Composable
internal fun EditorTextArea(
    state: RexGitUiState,
    editor: RexGitEditorState,
    modifier: Modifier = Modifier
) {
        return __splitImpl_EditorTextArea(state, editor, modifier)
    }

@Composable
internal fun EditorKeyChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
        return __splitImpl_EditorKeyChip(label, onClick, modifier)
    }

@Composable
internal fun EditorStatusText(
    editor: RexGitEditorState,
    modifier: Modifier = Modifier
) {
        return __splitImpl_EditorStatusText(editor, modifier)
    }

/** Search + go-to-line dialogs. */
@Composable
internal fun EditorDialogs(editor: RexGitEditorState) {
        return __splitImpl_EditorDialogs(editor)
    }
