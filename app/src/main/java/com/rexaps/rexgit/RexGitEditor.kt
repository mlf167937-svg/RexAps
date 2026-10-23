
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.res.Configuration
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import java.io.File


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
private const val FONT_APPLY_DELAY_MS = 350L

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

internal class EditorAction(
    val label: String,
    val icon: ImageVector?,
    val onClick: () -> Unit
)

@Composable
internal fun rememberEditorActions(editor: RexGitEditorState): List<EditorAction> {
    val clipboard = LocalClipboardManager.current

    return remember(editor, clipboard) {
        listOf(
            EditorAction("Search", Icons.Default.Search) {
                editor.searchOpen.value = true
            },
            EditorAction("Go to line", null) {
                editor.goToLineText.value = "${editor.currentLine()}"
                editor.goToLineOpen.value = true
            },
            EditorAction("Copy all", Icons.Default.ContentCopy) {
                clipboard.setText(AnnotatedString(editor.textField.text))
                editor.message("All text copied")
            },
            EditorAction("Paste", Icons.Default.ContentPaste) {
                val text = clipboard.getText()?.text.orEmpty()
                if (text.isNotEmpty()) editor.insert(text) else editor.message("Clipboard is empty")
            },
            EditorAction("Select all", Icons.Default.SelectAll) {
                editor.selectAll()
            }
        )
    }
}

@Composable
internal fun EditorToolButton(
    action: EditorAction,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = action.onClick,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = modifier.heightIn(min = 40.dp)
    ) {
        if (action.icon != null) {
            Icon(action.icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(action.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun EditorHeader(
    state: RexGitUiState,
    editor: RexGitEditorState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    compact: Boolean = false
) {
    val path = state.editorPath ?: return
    val scheme = MaterialTheme.colorScheme

    val repoPath = state.selectedRepository?.path ?: ""
    val name = path.substringAfterLast('/')
    val relative = path.removePrefix(repoPath).trimStart('/')
    val language = remember(path) { RexGitSyntax.detect(path) }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = if (compact) 6.dp else 12.dp,
                bottom = if (compact) 4.dp else 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderIconButton(
            icon = Icons.Default.ArrowBack,
            description = "Close editor",
            onClick = onBack
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (state.editorDirty) "Unsaved changes" else relative,
                style = MaterialTheme.typography.labelMedium,
                color = if (state.editorDirty) scheme.tertiary else scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            RexGitSyntax.label(language),
            style = MaterialTheme.typography.labelMedium,
            color = scheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        HeaderIconButton(
            icon = Icons.Default.Search,
            description = "Search",
            onClick = { editor.searchOpen.value = true }
        )

        Spacer(Modifier.width(6.dp))

        Button(
            onClick = onSave,
            enabled = state.editorDirty && !state.isSaving,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.heightIn(min = if (compact) 40.dp else 44.dp)
        ) {
            Text(if (state.isSaving) "Saving" else "Save")
        }
    }
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
    val scheme = MaterialTheme.colorScheme
    val draft = editor.draftPx
    val pending = editor.fontPending

    @Composable
    fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(10.dp),
            color = scheme.surfaceVariant
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }

    @Composable
    fun PxSlider(sliderModifier: Modifier) {
        Slider(
            value = draft.toFloat(),
            onValueChange = {
                editor.setSliding(true)
                editor.setDraftPx(it.toInt())
            },
            onValueChangeFinished = { editor.setSliding(false) },
            valueRange = EDITOR_MIN_FONT.toFloat()..EDITOR_MAX_FONT.toFloat(),
            modifier = sliderModifier
        )
    }

    Column(modifier) {
        if (stacked) {
            PxSlider(Modifier.fillMaxWidth())

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepButton("A−", draft > EDITOR_MIN_FONT) { editor.setDraftPx(draft - 1) }

                Text(
                    "${draft}px",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (pending) scheme.primary else scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                StepButton("A+", draft < EDITOR_MAX_FONT) { editor.setDraftPx(draft + 1) }
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepButton("A−", draft > EDITOR_MIN_FONT) { editor.setDraftPx(draft - 1) }

                PxSlider(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                StepButton("A+", draft < EDITOR_MAX_FONT) { editor.setDraftPx(draft + 1) }

                Spacer(Modifier.width(8.dp))

                Text(
                    "${draft}px",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (pending) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier.width(46.dp)
                )
            }
        }

        // Thin bar while the new px is waiting to be applied.
        ProgressSlot(pending)
    }
}

@Composable
internal fun EditorTextArea(
    state: RexGitUiState,
    editor: RexGitEditorState,
    modifier: Modifier = Modifier
) {
    val path = state.editorPath ?: return
    val scheme = MaterialTheme.colorScheme

    val language = remember(path) { RexGitSyntax.detect(path) }
    val field = editor.textField
    val fontPx = editor.fontPx
    val editorScroll = remember(path) { androidx.compose.foundation.ScrollState(0) }
    val lineCount = field.text.count { it == '\n' } + 1
    val gutterWidth = when {
        lineCount >= 1000 -> 44.dp
        lineCount >= 100 -> 36.dp
        else -> 28.dp
    }

    val syntaxColors = RexGitSyntaxColors(
        keyword = scheme.primary,
        string = scheme.tertiary,
        number = scheme.secondary,
        comment = scheme.onSurfaceVariant.copy(alpha = 0.72f),
        type = scheme.primary.copy(alpha = 0.82f),
        property = scheme.secondary,
        tag = scheme.primary,
        punctuation = scheme.onSurfaceVariant
    )

    val highlighted = remember(
        field.text,
        language,
        syntaxColors
    ) {
        RexGitSyntax.highlight(field.text, language, syntaxColors)
    }

    val visualTransformation = remember(highlighted) {
        VisualTransformation { transformed ->
            TransformedText(
                highlighted,
                androidx.compose.ui.text.input.OffsetMapping.Identity
            )
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant)
    ) {
        Row(Modifier.fillMaxSize()) {
            // A synchronized, low-contrast gutter keeps code line numbers visible while scrolling.
            Column(
                modifier = Modifier
                    .width(gutterWidth)
                    .fillMaxHeight()
                    .verticalScroll(editorScroll)
                    .padding(start = 8.dp, top = 14.dp, bottom = 14.dp, end = 6.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    (1..lineCount).joinToString("\n"),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.72f),
                        fontSize = fontPx.sp,
                        lineHeight = (fontPx * 1.4f).sp
                    ),
                    maxLines = Int.MAX_VALUE,
                    softWrap = false
                )
            }
            BasicTextField(
                value = field,
                onValueChange = editor::onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = scheme.onSurface,
                    fontSize = fontPx.sp,
                    lineHeight = (fontPx * 1.4f).sp
                ),
                visualTransformation = visualTransformation,
                cursorBrush = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    autoCorrect = false
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(editorScroll)
                    .focusRequester(editor.focusRequester)
                    .padding(top = 14.dp, bottom = 14.dp, end = 14.dp)
            )
        }
    }
}

@Composable
internal fun EditorKeyChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
internal fun EditorStatusText(
    editor: RexGitEditorState,
    modifier: Modifier = Modifier
) {
    Text(
        "${editor.lines} lines  •  ${editor.textField.text.length} characters",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/** Search + go-to-line dialogs. */
@Composable
internal fun EditorDialogs(editor: RexGitEditorState) {
    val scheme = MaterialTheme.colorScheme

    if (editor.searchOpen.value) {
        AlertDialog(
            onDismissRequest = { editor.searchOpen.value = false },
            title = { Text("Search in file") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editor.searchQuery.value,
                        onValueChange = {
                            editor.searchQuery.value = it
                            // A new query starts before the first match so Next
                            // selects result #1 instead of unintentionally skipping it.
                            editor.searchIndex.value = -1
                        },
                        singleLine = true,
                        label = { Text("Find") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    val query = editor.searchQuery.value
                    val count = editor.searchMatches(query).size
                    Text(
                        if (query.isBlank()) "Type text to search" else "$count match${if (count == 1) "" else "es"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { editor.selectSearchMatch(-1) }, enabled = query.isNotBlank()) { Text("Previous") }
                        OutlinedButton(onClick = { editor.selectSearchMatch(1) }, enabled = query.isNotBlank()) { Text("Next") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { editor.searchOpen.value = false }) { Text("Done") } }
        )
    }

    if (editor.goToLineOpen.value) {
        AlertDialog(
            onDismissRequest = { editor.goToLineOpen.value = false },
            title = { Text("Go to line") },
            text = {
                OutlinedTextField(
                    value = editor.goToLineText.value,
                    onValueChange = { editor.goToLineText.value = it.filter(Char::isDigit) },
                    singleLine = true,
                    label = { Text("Line 1–${editor.lines}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = { TextButton(onClick = editor::goToLine) { Text("Go") } },
            dismissButton = { TextButton(onClick = { editor.goToLineOpen.value = false }) { Text("Cancel") } }
        )
    }
}
