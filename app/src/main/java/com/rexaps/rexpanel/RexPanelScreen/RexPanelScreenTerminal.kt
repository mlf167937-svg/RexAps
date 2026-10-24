package com.rexaps.rexpanel

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun TerminalPane(
    vm: RexPanelViewModel,
    fontSize: Int,
    fullscreen: Boolean,
    onToggleFull: () -> Unit,
    onFontSize: (Int) -> Unit,
    modifier: Modifier
) {
    val colors =
        MaterialTheme.colorScheme

    val density =
        LocalDensity.current

    val measurer =
        rememberTextMeasurer()

    val style =
        remember(fontSize) {
            TextStyle(
                fontFamily =
                    FontFamily.Monospace,
                fontSize =
                    fontSize.sp,
                lineHeight =
                    (fontSize * 1.35f).sp
            )
        }

    val cell =
        remember(style) {
            measurer.measure(
                "W",
                style
            ).size
        }

    val rowH =
        with(density) {
            cell.height.toDp()
        }

    val termBg =
        lerp(
            colors.background,
            Color.Black,
            0.55f
        )

    val focus =
        remember {
            FocusRequester()
        }

    val keyboard =
        LocalSoftwareKeyboardController
            .current

    val term =
        vm.term

    val version =
        term.version

    Column(modifier) {

        Row(
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                "Terminal",
                style =
                    MaterialTheme.typography
                        .titleSmall,
                modifier =
                    Modifier.weight(1f)
            )

            TextButton(
                onClick = {
                    onFontSize(
                        (
                            fontSize - 1
                            ).coerceAtLeast(8)
                    )
                }
            ) {
                Text("A−")
            }

            TextButton(
                onClick = {
                    onFontSize(
                        (
                            fontSize + 1
                            ).coerceAtMost(22)
                    )
                }
            ) {
                Text("A+")
            }

            IconButton(
                onClick = onToggleFull
            ) {

                Icon(
                    if (fullscreen) {
                        Icons.Default.FullscreenExit
                    } else {
                        Icons.Default.Fullscreen
                    },
                    if (fullscreen) {
                        "Keluar fullscreen"
                    } else {
                        "Fullscreen"
                    }
                )
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(
                    horizontal =
                        if (fullscreen) {
                            0.dp
                        } else {
                            12.dp
                        }
                )
                .clip(
                    RoundedCornerShape(
                        if (fullscreen) {
                            0.dp
                        } else {
                            20.dp
                        }
                    )
                )
                .background(termBg)
                .clipToBounds()
                .clickable(
                    interactionSource =
                        remember {
                            MutableInteractionSource()
                        },
                    indication = null
                ) {
                    focus.requestFocus()
                    keyboard?.show()
                }
                .padding(8.dp)
                .onSizeChanged { size ->

                    val c =
                        size.width /
                            cell.width

                    val r =
                        size.height /
                            cell.height

                    if (
                        c >= 10 &&
                        r >= 3
                    ) {
                        vm.resize(
                            c,
                            r
                        )
                    }
                }
        ) {

            Column {

                for (
                    r in 0 until term.rows
                ) {

                    Text(
                        text =
                            term.rowText(
                                version,
                                r,
                                TermFg,
                                colors.primary,
                                termBg
                            ),
                        style = style,
                        color = TermFg,
                        softWrap = false,
                        maxLines = 1,
                        overflow =
                            TextOverflow.Clip,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(rowH)
                    )
                }
            }

            // Input tersembunyi untuk
            // menangkap keyboard.
            BasicTextField(
                value = "",
                onValueChange = {
                    if (it.isNotEmpty()) {
                        vm.sendText(it)
                    }
                },
                modifier =
                    Modifier
                        .size(1.dp)
                        .alpha(0f)
                        .focusRequester(focus)
                        .onPreviewKeyEvent {
                            handleKey(
                                it,
                                vm
                            )
                        },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                            KeyboardType.Text,
                        imeAction =
                            ImeAction.Send
                    ),
                keyboardActions =
                    KeyboardActions(
                        onSend = {
                            vm.sendRaw(
                                "\r"
                            )
                        }
                    )
            )
        }
    }
}
