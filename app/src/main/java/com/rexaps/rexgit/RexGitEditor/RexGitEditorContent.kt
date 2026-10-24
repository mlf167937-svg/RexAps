@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun __splitImpl_EditorTextArea(
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

/**
 * px controls. They only change the draft; the text is re-laid out once,
 * after the controls have been idle (see rememberRexGitEditorState).
 *
 * [stacked] puts the slider on its own line (narrow side panel in landscape).
 */
@Composable
internal fun __splitImpl_EditorFontControls(
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
internal fun __splitImpl_EditorKeyChip(
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
