package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexcoder.model.EditorGroup
import com.rexaps.rexcoder.model.MAX_EDITOR_FONT
import com.rexaps.rexcoder.model.MIN_EDITOR_FONT
import com.rexaps.rexcoder.theme.Rex

// ───────────── Font size menu (dipakai editor, tree, terminal) ─────────────

/** Isi menu: judul, stepper − / + dan deretan preset. */
@Composable
internal fun FontStepperContent(
    label: String, value: Int, min: Int, max: Int, presets: List<Int>, onChange: (Int) -> Unit
) {
    Column {
        Text(
            "FONT $label  ($min–$max)", fontSize = 10.sp, letterSpacing = 1.2.sp, color = Rex.TextDim,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp)
        )
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallIconButton(Icons.Outlined.Remove, "Kecilkan font $label", 40.dp, Rex.Text) {
                if (value > min) onChange(value - 1)
            }
            Text(
                "${value}px", fontSize = 16.sp, fontFamily = FontFamily.Monospace, color = Rex.TextBright,
                textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 64.dp)
            )
            SmallIconButton(Icons.Outlined.Add, "Besarkan font $label", 40.dp, Rex.Text) {
                if (value < max) onChange(value + 1)
            }
        }
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { p ->
                val sel = p == value
                Text(
                    "$p", fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                    color = if (sel) Rex.OnAccent else Rex.Text,
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (sel) Rex.Accent else Rex.Field)
                        .clickable(role = Role.Button, onClickLabel = "Set $p px") { onChange(p) }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }
    }
}

/** "E2 12px" chip. Tap → stepper + presets for this editor's own font size. */
@Composable
internal fun FontSizeChip(label: String, group: EditorGroup, focused: Boolean, onChange: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    Box {
        Row(
            Modifier
                .padding(horizontal = 2.dp)
                .height(26.dp)
                .clip(shape)
                .border(1.dp, if (focused) Rex.Accent.copy(alpha = 0.7f) else Rex.Border, shape)
                .clickable(role = Role.Button, onClickLabel = "Ubah ukuran font $label") { open = true }
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Rex.Accent)
            Spacer(Modifier.width(5.dp))
            Text("${group.fontSize}px", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Rex.Text, maxLines = 1)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(Rex.Overlay)) {
            FontStepperContent(
                label, group.fontSize, MIN_EDITOR_FONT, MAX_EDITOR_FONT,
                listOf(2, 4, 6, 8, 10, 12, 13, 14, 16, 18, 20, 22), onChange
            )
        }
    }
}
