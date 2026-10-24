
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.rexaps.rexgit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.only

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
