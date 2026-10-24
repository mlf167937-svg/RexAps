//RexCoderApp.kt
package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.theme.Rex

@Composable
internal fun TitleBar(s: RexCoderState) {
    Row(
        Modifier.fillMaxWidth().height(36.dp).background(Rex.TitleBar)
            .drawBehind { drawRect(Rex.accentBrush(), Offset(0f, size.height - 1.dp.toPx()), Size(size.width, 1.dp.toPx()), alpha = 0.6f) }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Code, null, tint = Rex.Accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("REX", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Rex.Accent)
        Text("CODER", fontSize = 12.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp, color = Rex.TextBright)
        Spacer(Modifier.width(10.dp))
        listOf("File", "Edit", "View", "Terminal").forEach { m ->
            Text(
                m, fontSize = 12.sp, color = Rex.Text,
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable {
                    when (m) {
                        "View" -> s.sidebarVisible = !s.sidebarVisible
                        "Terminal" -> s.panelVisible = !s.panelVisible
                        "File" -> s.save()
                        else -> s.paletteVisible = true
                    }
                }.padding(horizontal = 8.dp, vertical = 7.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier.weight(1f).widthIn(max = 320.dp).height(26.dp).clip(RoundedCornerShape(8.dp))
                .background(Rex.Field).border(1.dp, Rex.Border, RoundedCornerShape(8.dp))
                .clickable { s.paletteVisible = true }.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Search, null, tint = Rex.TextDim, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("RexCoder Project", fontSize = 12.sp, color = Rex.TextDim, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        SmallIconButton(Icons.Outlined.Save, "Save", 32.dp) { s.save() }
    }
}

// ───────────── Portrait phone ─────────────

@Composable
internal fun CompactTopBar(s: RexCoderState, onMenu: () -> Unit) {
    val doc = s.focusedDoc
    Row(
        Modifier.fillMaxWidth().height(48.dp).background(Rex.TitleBar)
            .drawBehind { drawRect(Rex.accentBrush(), Offset(0f, size.height - 1.dp.toPx()), Size(size.width, 1.dp.toPx()), alpha = 0.6f) }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SmallIconButton(Icons.Outlined.Menu, "Open explorer", 44.dp, Rex.Text, onMenu)
        Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
            Text(
                (doc?.name ?: "RexCoder") + if (doc?.modified == true) " ●" else "",
                fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Rex.TextBright, maxLines = 1
            )
            Text("${s.groups.size} editor" + if (s.groups.size > 1) "s" else "", fontSize = 11.sp, color = Rex.TextDim)
        }
        SmallIconButton(Icons.Outlined.Search, "Command palette", 40.dp, Rex.Text) { s.paletteVisible = true }
        LayoutMenu(s, tint = Rex.Text, size = 40.dp)
        SmallIconButton(Icons.Outlined.Save, "Save", 40.dp, Rex.Text) { s.save() }
    }
}
