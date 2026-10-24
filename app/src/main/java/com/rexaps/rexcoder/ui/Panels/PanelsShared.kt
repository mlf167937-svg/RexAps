//Panels.kt
package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.theme.Rex

internal val monoSmall: TextStyle
    get() = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Rex.Text)

internal val extraKeys = listOf(
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
