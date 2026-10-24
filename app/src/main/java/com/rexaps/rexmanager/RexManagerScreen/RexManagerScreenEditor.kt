package com.rexaps.rexmanager

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun EditorScreen(file: File, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val tooBig = file.length() > MAX_EDIT_BYTES
    var loaded by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf(TextFieldValue("")) }
    var original by remember { mutableStateOf("") }
    var fontPx by rememberSaveable { mutableFloatStateOf(14f) } // 4..22
    var highlight by rememberSaveable { mutableStateOf(true) }
    var confirmExit by remember { mutableStateOf(false) }
    val dirty = loaded && value.text != original

    LaunchedEffect(file) {
        if (!tooBig) {
            val t = withContext(Dispatchers.IO) { runCatching { file.readText() }.getOrDefault("") }
            original = t
            value = TextFieldValue(t)
            loaded = true
            if (file.length() > HIGHLIGHT_LIMIT) highlight = false
        }
    }

    fun tryBack() {
        if (dirty) confirmExit = true else onBack()
    }
    BackHandler(onBack = ::tryBack)

    Column(Modifier.fillMaxSize()) {
        RexTopBar(
            title = file.name,
            subtitle = if (dirty) "Belum disimpan" else formatSize(file.length()),
            onBack = ::tryBack,
            actions = {
                IconButton(
                    enabled = dirty,
                    onClick = {
                        scope.launch {
                            val text = value.text
                            withContext(Dispatchers.IO) { file.writeText(text) }
                            original = text
                            Toast.makeText(ctx, "Tersimpan", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Filled.Save, "Simpan", tint = if (dirty) Rex.primary else Rex.muted)
                }
            }
        )

        if (tooBig) {
            Text(
                "File lebih dari 2 MB, terlalu besar untuk editor.",
                color = Rex.muted, modifier = Modifier.padding(16.dp)
            )
        } else {
            // Kontrol ukuran font 4–22 px
            Row(
                Modifier.fillMaxWidth().background(Rex.surface).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { fontPx = (fontPx - 1f).coerceAtLeast(4f) }) {
                    Icon(Icons.Filled.Remove, "Perkecil teks", tint = Rex.onSurface)
                }
                Slider(
                    value = fontPx,
                    onValueChange = { fontPx = it.toInt().toFloat() },
                    valueRange = 4f..22f,
                    steps = 17,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { fontPx = (fontPx + 1f).coerceAtMost(22f) }) {
                    Icon(Icons.Filled.Add, "Perbesar teks", tint = Rex.onSurface)
                }
                Text(
                    "${fontPx.toInt()}px", color = Rex.muted, fontSize = 12.sp,
                    modifier = Modifier.widthIn(min = 36.dp)
                )
                Switch(
                    checked = highlight,
                    onCheckedChange = { highlight = it },
                    modifier = Modifier.semantics { contentDescription = "Syntax highlight" }
                )
            }

            if (!loaded) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val transformation = remember(file.name, highlight) { RexHighlighter(file.name, highlight) }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = { value = it },
                        textStyle = TextStyle(
                            color = Rex.onSurface,
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontPx.sp,
                            lineHeight = (fontPx * 1.4f).sp
                        ),
                        cursorBrush = SolidColor(Rex.primary),
                        visualTransformation = transformation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 300.dp)
                    )
                }
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Perubahan belum disimpan") },
            text = { Text("Keluar tanpa menyimpan?") },
            confirmButton = {
                TextButton(onClick = { confirmExit = false; onBack() }) {
                    Text("Keluar", color = Rex.danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Lanjut edit") } }
        )
    }
}
