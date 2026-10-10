//Panels.kt
package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.librex.rexcode.RexCode
import com.rexaps.rexcoder.model.MAX_TERMINAL_FONT
import com.rexaps.rexcoder.model.MIN_TERMINAL_FONT
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.runtime.AnsiText
import com.rexaps.rexcoder.theme.Rex

// ───────────── Bottom panel: Problems / Output / Terminal ─────────────

private val monoSmall: TextStyle
    get() = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Rex.Text)

@Composable
fun BottomPanel(s: RexCoderState, modifier: Modifier = Modifier) {
    var tab by rememberSaveable { mutableIntStateOf(2) }
    Column(modifier.background(Rex.EditorBg)) {
        Row(Modifier.fillMaxWidth().height(34.dp).padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("PROBLEMS", "OUTPUT", "TERMINAL").forEachIndexed { i, t ->
                Text(
                    t, fontSize = 11.sp, letterSpacing = 0.8.sp, color = if (i == tab) Rex.TextBright else Rex.TextDim,
                    modifier = Modifier.clickable { tab = i }
                        .drawBehind { if (i == tab) drawRect(Rex.accentBrush(), Offset(8.dp.toPx(), size.height - 1.5.dp.toPx()), Size(size.width - 16.dp.toPx(), 1.5.dp.toPx())) }
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            val target = s.sshTarget
            if (s.sshConnected && target != null) {
                Text(
                    "● ${target.user}@${target.host}", fontSize = 11.sp, color = Rex.Success,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 150.dp).padding(end = 4.dp)
                )
                Text(
                    "^C", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Rex.Warning,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .clickable(role = Role.Button, onClickLabel = "Kirim Ctrl+C") { s.sendInterrupt() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
            FontMenuButton(
                title = "TERM", value = s.terminalFontSize,
                min = MIN_TERMINAL_FONT, max = MAX_TERMINAL_FONT,
                presets = listOf(6, 8, 10, 12, 14, 16, 18, 20)
            ) { s.setTerminalFont(it) }
            if (s.sshConnected) {
                SmallIconButton(Icons.Outlined.LinkOff, "Disconnect SSH", 32.dp, Rex.Error) { s.disconnectSsh() }
            } else {
                SmallIconButton(Icons.Outlined.Link, "SSH login", 32.dp, Rex.Accent) { s.openSshLogin() }
            }
            SmallIconButton(Icons.Outlined.DeleteSweep, "Clear terminal", 32.dp) { s.clearTerminal() }
            SmallIconButton(Icons.Outlined.Close, "Close panel") { s.panelVisible = false }
        }
        HDivider()
        when (tab) {
            0 -> Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, null, tint = Rex.Success, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp)); Text("No problems have been detected in the workspace.", fontSize = 12.sp, color = Rex.TextDim)
            }
            1 -> LazyColumn(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                item { Text("[RexCoder] Runtime manager ready", style = monoSmall) }
                item { Text("[RexCoder] Workspace: ${com.rexaps.rexcoder.storage.WorkspaceManager.DISPLAY_PATH}", style = monoSmall) }
                item { Text("[RexCoder] .language runtimes are discovered automatically", style = monoSmall) }
            }
            else -> TerminalView(s)
        }
    }
    if (s.sshLoginVisible) {
        SshLoginDialog(s)
    }
}

@Composable
private fun SshLoginDialog(s: RexCoderState) {
    var showPassword by rememberSaveable { mutableStateOf(false) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { s.sshLoginVisible = false },
        containerColor = Rex.Overlay,
        titleContentColor = Rex.TextBright,
        textContentColor = Rex.Text,
        title = { Text("RexCoder SSH") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Login SSH seperti RexPanel, tetapi terminal tetap berada di dalam RexCoder.", fontSize = 12.sp, color = Rex.TextDim)
                androidx.compose.material3.OutlinedTextField(
                    value = s.sshCommand,
                    onValueChange = { s.sshCommand = it },
                    singleLine = true,
                    label = { Text("SSH command") },
                    placeholder = { Text("ssh -p 8022 user@192.168.0.101") }
                )
                androidx.compose.material3.OutlinedTextField(
                    value = s.sshPassword,
                    onValueChange = { s.sshPassword = it },
                    singleLine = true,
                    label = { Text("Password") },
                    visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                if (showPassword) "Sembunyikan password" else "Tampilkan password"
                            )
                        }
                    }
                )
                // Auto `cd` into the RexCoder workspace right after a successful login.
                Row(
                    Modifier.fillMaxWidth()
                        .toggleable(value = s.sshAutoCd, role = Role.Checkbox, onValueChange = { s.sshAutoCd = it }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = s.sshAutoCd, onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = Rex.Accent, checkmarkColor = Rex.OnAccent, uncheckedColor = Rex.TextDim)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Auto cd ke workspace", fontSize = 13.sp, color = Rex.Text)
                        Text(com.rexaps.rexcoder.storage.WorkspaceManager.DISPLAY_PATH, fontSize = 11.sp, color = Rex.TextDim)
                    }
                }
                Text("Format: ssh -p PORT user@host", fontSize = 11.sp, color = Rex.TextDim)
            }
        },
        confirmButton = {
            TextButton(onClick = { s.connectSsh(s.sshCommand, s.sshPassword) }) { Text("Connect", color = Rex.Accent) }
        },
        dismissButton = {
            TextButton(onClick = { s.sshLoginVisible = false }) { Text("Cancel", color = Rex.TextDim) }
        }
    )
}

@Composable
private fun TerminalView(s: RexCoderState) {
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

// ───────────── Status bar ─────────────

@Composable
fun StatusBar(s: RexCoderState, compact: Boolean) {
    val doc = s.focusedDoc
    val t = TextStyle(fontSize = 12.sp, color = Rex.Text)
    val group = s.focusedGroup
    val groupIndex = s.groups.indexOf(group) + 1
    Row(
        Modifier.fillMaxWidth().height(24.dp).background(Rex.ActivityBar)
            .drawBehind { drawRect(Rex.accentBrush(), size = Size(size.width, 1.dp.toPx()), alpha = 0.7f) }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.AccountTree, null, tint = Rex.Accent, modifier = Modifier.size(14.dp))
        Text(" RexCoder", style = t.copy(fontWeight = FontWeight.Medium, color = Rex.TextBright))
        Spacer(Modifier.width(10.dp))
        Icon(Icons.Outlined.ErrorOutline, null, tint = Rex.TextDim, modifier = Modifier.size(14.dp))
        Text(" 0  ", style = t)
        Icon(Icons.Outlined.WarningAmber, null, tint = Rex.TextDim, modifier = Modifier.size(14.dp))
        Text(" 0", style = t)
        val target = s.sshTarget
        if (s.sshConnected && target != null && !compact) {
            Text("   ● ${target.user}@${target.host}", style = t.copy(color = Rex.Success), maxLines = 1)
        }
        Spacer(Modifier.weight(1f))
        if (doc != null) {
            val before = doc.value.text.take(doc.value.selection.start)
            val ln = before.count { it == '\n' } + 1
            val col = before.length - (before.lastIndexOf('\n') + 1) + 1
            Text("E$groupIndex ${group.fontSize}px   ", style = t.copy(color = Rex.Accent))
            Text("Ln $ln, Col $col", style = t)
            if (!compact) Text("   Spaces: 4   UTF-8   ", style = t) else Spacer(Modifier.width(10.dp))
            Text(RexCode.language(doc.name).displayName, style = t)
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Outlined.Terminal, "Toggle terminal", tint = Rex.Accent,
            modifier = Modifier.size(16.dp).clickable { s.panelVisible = !s.panelVisible }
        )
    }
}

// ───────────── Extra keys (muncul saat keyboard terbuka) ─────────────

private val extraKeys = listOf(
    "TAB" to "    ", "{" to "{", "}" to "}", "(" to "(", ")" to ")", "[" to "[", "]" to "]",
    "<" to "<", ">" to ">", ";" to ";", ":" to ":", "\"" to "\"", "'" to "'", "=" to "=",
    "/" to "/", "\\" to "\\", "|" to "|", "&" to "&", "!" to "!", "_" to "_"
)

@Composable
fun ExtraKeysBar(s: RexCoderState) {
    val focus = LocalFocusManager.current
    Row(
        Modifier.fillMaxWidth().background(Rex.TabInactive).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp)) {
            extraKeys.forEach { (label, insert) ->
                Box(
                    Modifier.padding(horizontal = 2.dp).defaultMinSize(minWidth = 40.dp).height(38.dp)
                        .clip(RoundedCornerShape(8.dp)).background(Rex.Field)
                        .border(1.dp, Rex.Border, RoundedCornerShape(8.dp))
                        .clickable { s.insertAtCursor(insert) }.padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) { Text(label, fontSize = 15.sp, color = Rex.Text, fontFamily = FontFamily.Monospace) }
            }
        }
        SmallIconButton(Icons.Outlined.KeyboardHide, "Hide keyboard", 44.dp) { focus.clearFocus() }
    }
}
