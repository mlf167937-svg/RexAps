package com.rexaps.rexcoder.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Html
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.rexaps.rexcoder.theme.Rex

/** Original RexCoder language badge set; not copied from third-party icon packs. */
@Composable
fun RexLanguageIcon(fileName: String, modifier: Modifier = Modifier) {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    val label = when (ext) {
        "py" -> "Py"; "js", "mjs", "cjs" -> "JS"; "ts" -> "TS"; "tsx" -> "TX"; "jsx" -> "JX"
        "kt", "kts" -> "K"; "java" -> "J"; "html", "htm" -> "H"; "css", "scss", "sass", "less" -> "#"
        "json" -> "{}"; "xml" -> "<>"; "yaml", "yml" -> "Y"; "md" -> "M"; "rs" -> "Rs"; "go" -> "Go"
        "php" -> "PHP"; "rb" -> "Rb"; "dart" -> "D"; "swift" -> "Sw"; "sql" -> "SQL"; "sh", "bash" -> "$"
        "vue" -> "V"; "c" -> "C"; "cpp", "cc", "cxx" -> "C++"; "cs" -> "C#"; "toml", "ini", "env" -> "="
        "gradle", "dockerfile" -> "◆"; else -> null
    }
    if (label == null) Icon(Icons.Outlined.Description, null, tint = Rex.TextDim, modifier = modifier)
    else Text(label, color = languageColor(ext), fontSize = 10.sp, maxLines = 1, modifier = modifier)
}

private fun languageColor(ext: String): Color = when (ext) {
    "py" -> Color(0xFFFFD54F); "js", "mjs", "cjs" -> Color(0xFFF7DF1E); "ts", "tsx" -> Color(0xFF3178C6)
    "kt", "kts" -> Color(0xFFB77CFF); "java" -> Color(0xFFE76F51); "html", "htm" -> Color(0xFFE34C26)
    "css", "scss", "sass", "less" -> Color(0xFF42A5F5); "json" -> Color(0xFFCBCB41); "rs" -> Color(0xFFCE9178)
    "go" -> Color(0xFF67D5E8); "php" -> Color(0xFF9B9BFF); "rb" -> Color(0xFFCC342D); "swift" -> Color(0xFFF05138)
    else -> Rex.Text
}
