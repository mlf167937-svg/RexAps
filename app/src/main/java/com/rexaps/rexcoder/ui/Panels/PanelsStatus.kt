//Panels.kt
package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.librex.rexcode.RexCode
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.theme.Rex

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
