
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.rexaps.rexgit

import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.layout.only
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

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
    private val draftFont: MutableState<Int>,
    private val slidingState: MutableState<Boolean>,
    private val selectionStart: MutableState<Int>,
    private val selectionEnd: MutableState<Int>,
    val searchOpen: MutableState<Boolean>,
    val searchQuery: MutableState<String>,
    val searchIndex: MutableState<Int>,
    val goToLineOpen: MutableState<Boolean>,
    val goToLineText: MutableState<String>,
    val focusRequester: FocusRequester,
    private val onEdit: (String) -> Unit,
    private val toast: (String) -> Unit
) {
    var textField by mutableStateOf(TextFieldValue(initialText, initialSelection))
        private set

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
        draftFont.value = px.coerceIn(EDITOR_MIN_FONT, EDITOR_MAX_FONT)
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
        val changed = new.text != textField.text

        textField = new

        if (new.selection.start != selectionStart.value || new.selection.end != selectionEnd.value) {
            selectionStart.value = new.selection.start
            selectionEnd.value = new.selection.end
        }

        if (changed) onEdit(new.text)
    }

    fun insert(snippet: String) {
        val sel = textField.selection
        val updated = textField.text.replaceRange(sel.min, sel.max, snippet)
        textField = TextFieldValue(updated, TextRange(sel.min + snippet.length))
        onEdit(updated)
    }

    fun selectAll() {
        textField = textField.copy(selection = TextRange(0, textField.text.length))
    }

    fun message(text: String) = toast(text)

    // ---------- search / go to line ----------

    fun searchMatches(query: String): List<Int> {
        if (query.isBlank()) return emptyList()
        val result = mutableListOf<Int>()
        var from = 0
        while (from < textField.text.length) {
            val found = textField.text.indexOf(query, from, ignoreCase = true)
            if (found < 0) break
            result += found
            from = found + maxOf(query.length, 1)
        }
        return result
    }

    fun selectSearchMatch(direction: Int) {
        val matches = searchMatches(searchQuery.value)
        if (matches.isEmpty()) {
            toast("No match")
            return
        }
        searchIndex.value = (searchIndex.value + direction).mod(matches.size)
        val start = matches[searchIndex.value]
        textField = textField.copy(selection = TextRange(start, start + searchQuery.value.length))
        focusRequester.requestFocus()
    }

    fun currentLine(): Int {
        val end = textField.selection.start.coerceIn(0, textField.text.length)
        return textField.text.substring(0, end).count { it == '\n' } + 1
    }

    fun goToLine() {
        val requested = goToLineText.value.toIntOrNull() ?: 1
        val targetLine = requested.coerceIn(1, lines)
        var currentLine = 1
        var offset = 0
        while (currentLine < targetLine) {
            val next = textField.text.indexOf('\n', offset)
            if (next < 0) break
            offset = next + 1
            currentLine++
        }
        textField = textField.copy(selection = TextRange(offset))
        focusRequester.requestFocus()
        goToLineOpen.value = false
    }
}

@Composable
internal fun rememberRexGitEditorState(
    path: String,
    viewModel: RexGitViewModel
): RexGitEditorState {
    val context = LocalContext.current

    // Always start at the default px so opening a file is light.
    val applied = rememberSaveable(path) { mutableStateOf(EDITOR_DEFAULT_FONT) }
    val draft = rememberSaveable(path) { mutableStateOf(applied.value) }
    val sliding = remember(path) { mutableStateOf(false) }
    val selStart = rememberSaveable(path) { mutableStateOf(0) }
    val selEnd = rememberSaveable(path) { mutableStateOf(0) }
    val searchOpen = rememberSaveable(path) { mutableStateOf(false) }
    val searchQuery = rememberSaveable(path) { mutableStateOf("") }
    val searchIndex = rememberSaveable(path) { mutableStateOf(-1) }
    val goToLineOpen = rememberSaveable(path) { mutableStateOf(false) }
    val goToLineText = rememberSaveable(path) { mutableStateOf("1") }

    val editor = remember(path) {
        val text = viewModel.editorText()

        RexGitEditorState(
            initialText = text,
            initialSelection = TextRange(
                selStart.value.coerceIn(0, text.length),
                selEnd.value.coerceIn(0, text.length)
            ),
            appliedFont = applied,
            draftFont = draft,
            slidingState = sliding,
            selectionStart = selStart,
            selectionEnd = selEnd,
            searchOpen = searchOpen,
            searchQuery = searchQuery,
            searchIndex = searchIndex,
            goToLineOpen = goToLineOpen,
            goToLineText = goToLineText,
            focusRequester = FocusRequester(),
            onEdit = viewModel::updateEditor,
            toast = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        )
    }

    // Apply the chosen px only when the picker has been idle for a moment.
    // snapshotFlow keeps the reads out of composition, so dragging the slider
    // never recomposes the whole screen.
    LaunchedEffect(editor) {
        snapshotFlow { Triple(draft.value, sliding.value, applied.value) }
            .collectLatest { (wanted, isSliding, current) ->
                if (!isSliding && wanted != current) {
                    delay(FONT_APPLY_DELAY_MS)
                    editor.applyDraftPx()
                }
            }
    }

    return editor
}

// ---------- parts used by both layouts ----------
