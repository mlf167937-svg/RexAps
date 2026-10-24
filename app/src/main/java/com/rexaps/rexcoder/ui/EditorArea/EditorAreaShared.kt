package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rexaps.rexcoder.theme.Rex

fun fileStyle(name: String): Pair<ImageVector, Color> = when (name.substringAfterLast('.', "").lowercase()) {
    "kt", "kts" -> Icons.Outlined.Code to Color(0xFFB380FF)
    "html", "htm" -> Icons.Outlined.Html to Color(0xFFE37933)
    "css", "scss", "sass", "less" -> Icons.Outlined.Css to Color(0xFF519ABA)
    "json" -> Icons.Outlined.DataObject to Color(0xFFCBCB41)
    "js", "mjs", "cjs", "ts", "tsx", "jsx" -> Icons.Outlined.Code to Color(0xFFF7DF1E)
    "py" -> Icons.Outlined.Code to Color(0xFFFFD54F)
    "java" -> Icons.Outlined.Code to Color(0xFFE76F51)
    "md" -> Icons.Outlined.Description to Color(0xFF42A5F5)
    else -> Icons.Outlined.Description to Rex.TextDim
}

// ───────────── Resize handle (drag to resize, double-tap to reset) ─────────────

/** Thickness of the touch area of every draggable divider. */
val HANDLE_THICKNESS: Dp = 12.dp
