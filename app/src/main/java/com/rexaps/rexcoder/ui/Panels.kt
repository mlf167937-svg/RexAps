package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.librex.rexcode.RexCode
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.theme.Rex

// ───────────── Bottom panel: Problems / Output / Terminal ─────────────

private val outputLines = listOf(
    "[RexCoder] Language server started",
    "[RexCoder] Indexed 6 files",
    "[Kotlin] Daemon ready (v2.0)"
)

@Composable
fun BottomPanel(s: RexCoderState, modifier: Modifier = Modifier) {
    var tab by remember { mutableIntStateOf(2) }
    Column(modifier.background(Rex.EditorBg)) {
        Row(Modifier.fillMaxWidth().height(34.dp).padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("PROBLEMS", "OUTPUT", "TERMINAL").forEachIndexed { i, t ->
                Text(
                    t, fontSize = 11.sp, color = if (i == tab) Rex.TextBright else Rex.TextDim,
                    modifier = Modifier
                        .clickable { tab = i }
                        .drawBehind {
                            if (i == tab) drawRect(
                                Rex.Accent, Offset(8.dp.toPx(), size.height - 1.dp.toPx()),
                                Size(size.width - 16.dp.toPx(), 1.dp.toPx())
                            )
                        }
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            SmallIconButton(Icons.Outlined.Close, "Close panel") { s.panelVisible = false }
        }
        HDivider()
        val mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp, color = Rex.Text)
        when (tab) {
            0 -> Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, null, tint = Rex.Success, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("No problems have been detected in the workspace.", fontSize = 12.sp, color = Rex.TextDim)
            }
            1 -> LazyColumn(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                items(outputLines) { Text(it, style = mono) }
            }
            else -> LazyColumn(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                item { Text("❯ ./gradlew assembleDebug", style = mono.copy(color = Rex.SynFunction)) }
                item { Text("> Task :app:compileDebugKotlin", style = mono) }
                item { Text("> Task :app:assembleDebug", style = mono) }
                item { Text("BUILD SUCCESSFUL in 12s", style = mono.copy(color = Rex.Success)) }
                item { Text("❯ ▌", style = mono.copy(color = Rex.SynFunction)) }
            }
        }
    }
}

// ───────────── Status bar ─────────────

@Composable
fun StatusBar(s: RexCoderState, compact: Boolean) {
    val doc = s.focusedDoc
    val t = TextStyle(fontSize = 12.sp, color = Color.White)
    Row(
        Modifier.fillMaxWidth().height(24.dp).background(Rex.Accent).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.AccountTree, null, tint = Color.White, modifier = Modifier.size(14.dp))
        Text(" RexCoder", style = t)
        Spacer(Modifier.width(10.dp))
        Icon(Icons.Outlined.ErrorOutline, null, tint = Color.White, modifier = Modifier.size(14.dp))
        Text(" 0  ", style = t)
        Icon(Icons.Outlined.WarningAmber, null, tint = Color.White, modifier = Modifier.size(14.dp))
        Text(" 0", style = t)
        Spacer(Modifier.weight(1f))
        if (doc != null) {
            val before = doc.value.text.take(doc.value.selection.start)
            val ln = before.count { it == '\n' } + 1
            val col = before.length - (before.lastIndexOf('\n') + 1) + 1
            Text("Ln $ln, Col $col", style = t)
            if (!compact) Text("   Spaces: 4   UTF-8   ", style = t) else Spacer(Modifier.width(10.dp))
            Text(RexCode.language(doc.name).displayName, style = t)
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Outlined.Terminal, "Toggle terminal", tint = Color.White,
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
                        .clip(RoundedCornerShape(5.dp)).background(Rex.TitleBar)
                        .clickable { s.insertAtCursor(insert) }.padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) { Text(label, fontSize = 15.sp, color = Rex.Text, fontFamily = FontFamily.Monospace) }
            }
        }
        SmallIconButton(Icons.Outlined.KeyboardHide, "Hide keyboard", 44.dp) { focus.clearFocus() }
    }
}
