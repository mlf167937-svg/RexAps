package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.librex.rexcode.RexCode
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.theme.Rex

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
