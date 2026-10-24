@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun __splitImpl_rememberRexGitEditorState(
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
