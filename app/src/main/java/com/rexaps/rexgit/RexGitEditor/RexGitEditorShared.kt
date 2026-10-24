@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

internal fun RexGitEditorState.__splitImpl_goToLine() {
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

internal fun RexGitEditorState.__splitImpl_selectSearchMatch(direction: Int) {
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

internal fun RexGitEditorState.__splitImpl_searchMatches(query: String): List<Int> {
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

internal fun RexGitEditorState.__splitImpl_onValueChange(new: TextFieldValue) {
        val changed = new.text != textField.text

        textField = new

        if (new.selection.start != selectionStart.value || new.selection.end != selectionEnd.value) {
            selectionStart.value = new.selection.start
            selectionEnd.value = new.selection.end
        }

        if (changed) onEdit(new.text)
    }

internal fun RexGitEditorState.__splitImpl_insert(snippet: String) {
        val sel = textField.selection
        val updated = textField.text.replaceRange(sel.min, sel.max, snippet)
        textField = TextFieldValue(updated, TextRange(sel.min + snippet.length))
        onEdit(updated)
    }

internal fun RexGitEditorState.__splitImpl_currentLine(): Int {
        val end = textField.selection.start.coerceIn(0, textField.text.length)
        return textField.text.substring(0, end).count { it == '\n' } + 1
    }

internal fun RexGitEditorState.__splitImpl_selectAll() {
        textField = textField.copy(selection = TextRange(0, textField.text.length))
    }

internal fun RexGitEditorState.__splitImpl_setDraftPx(px: Int) {
        draftFont.value = px.coerceIn(EDITOR_MIN_FONT, EDITOR_MAX_FONT)
    }
