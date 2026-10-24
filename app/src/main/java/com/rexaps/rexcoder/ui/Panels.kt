//Panels.kt
package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexcoder.model.MAX_TERMINAL_FONT
import com.rexaps.rexcoder.model.MIN_TERMINAL_FONT
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.theme.Rex

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
