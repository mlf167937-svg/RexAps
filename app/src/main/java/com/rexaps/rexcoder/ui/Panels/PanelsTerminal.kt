package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.runtime.AnsiText
import com.rexaps.rexcoder.theme.Rex

@Composable
internal fun TerminalView(s: RexCoderState) {
    val density = LocalDensity.current
    val scroll = rememberScrollState()
    val fontSp = s.terminalFontSize
    val dark = Rex.EditorBg.luminance() < 0.5f
    val textColor = Rex.Text
    val bgColor = Rex.EditorBg

    // Parse ANSI -> AnnotatedString berwarna. Di-cache; invalid saat jumlah baris / baris terakhir berubah.
    val count = s.terminalLines.size
    val last = s.terminalLines.lastOrNull()
    val rendered = remember(count, last, dark, textColor) {
        AnsiText.render(s.terminalLines.toList(), dark, textColor, bgColor)
    }

    val lineHeightSp = fontSp * 1.5f
    val style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = fontSp.sp, lineHeight = lineHeightSp.sp)

    // Hitung kolom/baris yang muat -> dikirim ke PTY SSH (htop, nano, dll ikut menyesuaikan).
    val measurer = rememberTextMeasurer()
    val cellW = remember(fontSp) { measurer.measure("M", style).size.width.coerceAtLeast(1) }
    val lineHpx = with(density) { lineHeightSp.sp.toPx() }.coerceAtLeast(1f)
    val padPx = with(density) { 24.dp.toPx() }
    var area by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(area, cellW, lineHpx) {
        if (area.width > 0 && area.height > 0) {
            s.resizeTerminal(((area.width - padPx) / cellW).toInt(), (area.height / lineHpx).toInt())
        }
    }

    // Selalu ikut ke bawah saat konten bertambah.
    LaunchedEffect(scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth().onSizeChanged { area = it }) {
            Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 12.dp, vertical = 8.dp)) {
                SelectionContainer {
                    Column {
                        rendered.forEachIndexed { i, line ->
                            val raw = s.terminalLines.getOrNull(i) ?: ""
                            val base = when {
                                raw.startsWith("error:") -> Rex.Error
                                raw.startsWith("[SSH]") || raw.startsWith("[RexCoder]") -> Rex.Accent
                                else -> Rex.Text
                            }
                            Text(
                                if (line.text.isEmpty()) AnnotatedString(" ") else line,
                                style = style, color = base
                            )
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (s.sshConnected) "remote ❯" else "❯", color = Rex.Accent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = s.terminalInput,
                onValueChange = { s.terminalInput = it },
                singleLine = true,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Rex.Text),
                cursorBrush = SolidColor(Rex.Accent),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { s.submitTerminal() }),
                modifier = Modifier.weight(1f)
                    .background(Rex.Field, RoundedCornerShape(8.dp))
                    .border(1.dp, Rex.Border, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 9.dp)
            )
            Spacer(Modifier.width(6.dp))
            SmallIconButton(Icons.Outlined.Send, "Run command", 40.dp, Rex.Accent) { s.submitTerminal() }
        }
    }
}
