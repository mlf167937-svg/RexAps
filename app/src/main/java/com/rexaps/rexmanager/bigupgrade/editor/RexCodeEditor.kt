package com.rexaps.rexmanager.bigupgrade.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private val RexBg = Color(0xFF07111F)
private val RexPanel = Color(0xFF101E31)
private val RexBlue = Color(0xFF398BFF)
private val RexViolet = Color(0xFF8B5CF6)

/** RexCode editor UI: internal editor, lightweight syntax highlighting, line numbers and save. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexCodeEditorScreen(
    initialPath: String? = null,
    onBack: () -> Unit,
    onSave: (path: String, text: String) -> Result<Unit> = { path, text ->
        runCatching { File(path).writeText(text) }
    }
) {
    var path by remember(initialPath) { mutableStateOf(initialPath.orEmpty()) }
    var editor by remember(initialPath) {
        mutableStateOf(TextFieldValue(initialPath?.let { runCatching { File(it).readText() }.getOrDefault("") } ?: ""))
    }
    var message by remember { mutableStateOf<String?>(null) }
    var dirty by remember { mutableStateOf(false) }
    val language = path.substringAfterLast('.', "txt").lowercase()

    Scaffold(containerColor = RexBg, topBar = {
        TopAppBar(
            title = { Column { Text("RexCode", fontWeight = FontWeight.Bold); Text(path.substringAfterLast('/').ifBlank { "Dokumen baru" }, fontSize = 12.sp, color = Color.LightGray) } },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Kembali") } },
            actions = {
                IconButton(onClick = { message = "Pencarian editor: gunakan Ctrl+F pada versi berikutnya" }) { Icon(Icons.Default.Search, "Cari") }
                IconButton(onClick = {
                    if (path.isBlank()) message = "Isi path file terlebih dahulu atau gunakan Simpan Sebagai."
                    else onSave(path, editor.text).fold({ dirty = false; message = "File tersimpan" }, { message = "Gagal menyimpan: ${it.message}" })
                }) { Icon(Icons.Default.Save, "Simpan", tint = RexBlue) }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = RexPanel)
        )
    }, snackbarHost = { SnackbarHost(remember { SnackbarHostState() }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = path, onValueChange = { path = it; dirty = true }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("Path file") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                Surface(color = RexViolet.copy(alpha = .2f), shape = MaterialTheme.shapes.medium) { Text(language.uppercase(), Modifier.padding(horizontal = 12.dp, vertical = 18.dp), color = Color(0xFFC4B5FD), fontSize = 12.sp) }
            }
            Spacer(Modifier.height(10.dp))
            Surface(Modifier.fillMaxSize(), color = RexPanel, shape = MaterialTheme.shapes.large) {
                Row(Modifier.fillMaxSize().padding(10.dp)) {
                    val lines = editor.text.count { it == '\n' } + 1
                    Column(Modifier.width(34.dp).verticalScroll(rememberScrollState())) {
                        repeat(lines) { Text("${it + 1}", color = Color(0xFF62738B), fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.height(20.dp)) }
                    }
                    BasicTextField(
                        value = editor,
                        onValueChange = { editor = it; dirty = true },
                        modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFFE2E8F0), fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 20.sp),
                        visualTransformation = VisualTransformation { value -> TransformedText(rexCodeHighlight(value.text, language), OffsetMapping.Identity) },
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(RexBlue),
                        decorationBox = { inner -> Box { if (editor.text.isEmpty()) Text("Mulai menulis dengan RexCode…", color = Color(0xFF64748B), fontFamily = FontFamily.Monospace); inner() } }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${editor.text.length} karakter · ${editor.text.count { it == '\n' } + 1} baris", color = Color(0xFF8293AB), fontSize = 11.sp)
                Text(if (dirty) "Belum disimpan" else "Tersimpan", color = if (dirty) Color(0xFFFBBF24) else Color(0xFF34D399), fontSize = 11.sp)
            }
            message?.let { Text(it, color = Color(0xFF93C5FD), modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp) }
        }
    }
}


/** Syntax highlighting utility driven by RexCode language-extension definitions. */
fun rexCodeHighlight(source: String, extension: String): AnnotatedString = buildAnnotatedString {
    val ext = extension.lowercase().removePrefix(".")
    val keywords = when (ext) {
        "kt", "kts" -> setOf("fun","val","var","class","object","return","if","else","when","for","while","import","package","suspend","private","public","override","data","interface")
        "java" -> setOf("class","public","private","static","void","return","new","import","package","if","else","for","while","interface")
        "js", "ts", "jsx", "tsx" -> setOf("const","let","var","function","return","class","new","if","else","async","await","import","export")
        "json" -> setOf("true","false","null")
        "py" -> setOf("def","class","return","if","elif","else","for","while","import","from","async","await","True","False","None")
        "xml", "html", "htm" -> setOf("html","head","body","div","span","script","style","meta","link")
        else -> setOf("true","false","null","return","class","function","if","else")
    }
    val regex = Regex("//[^\\n]*|#[^\\n]*|\"(?:\\\\.|[^\"])*\"|'(?:\\\\.|[^'])*'|\\b\\d+(?:\\.\\d+)?\\b|\\b[A-Za-z_][A-Za-z0-9_]*\\b")
    var cursor = 0
    regex.findAll(source).forEach { m ->
        append(source.substring(cursor, m.range.first))
        val token = m.value
        val color = when {
            token.startsWith("//") || token.startsWith("#") -> Color(0xFF718096)
            token.startsWith('"') || token.startsWith('\'') -> Color(0xFF86EFAC)
            token.firstOrNull()?.isDigit() == true -> Color(0xFFFCA5A5)
            token in keywords -> Color(0xFFC4B5FD)
            else -> Color(0xFFE2E8F0)
        }
        withStyle(SpanStyle(color = color, fontWeight = if (token in keywords) FontWeight.SemiBold else FontWeight.Normal)) { append(token) }
        cursor = m.range.last + 1
    }
    append(source.substring(cursor))
}
