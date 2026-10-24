package com.rexaps.rexmanager

import android.os.Build
import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.rexaps.librex.rexcode.RexCode

internal fun hasAccess(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true

internal fun formatSize(b: Long): String = when {
    b < 1024L -> "$b B"
    b < 1024L * 1024 -> "%.1f KB".format(b / 1024.0)
    b < 1024L * 1024 * 1024 -> "%.1f MB".format(b / 1048576.0)
    else -> "%.2f GB".format(b / 1073741824.0)
}

@Composable
internal fun drawerItemColors() = androidx.compose.material3.NavigationDrawerItemDefaults.colors(
    selectedContainerColor = Rex.primary.copy(alpha = 0.18f),
    selectedIconColor = Color(0xFF83A5FF),
    selectedTextColor = Rex.onSurface,
    unselectedContainerColor = Color.Transparent,
    unselectedIconColor = Rex.muted,
    unselectedTextColor = Rex.onSurface
)

// ───────────────────────── Editor (semua file, syntax RexCode) ─────────────────────────
internal fun tokenColor(type: String): Color {
    val t = type.uppercase()
    return when {
        "KEYWORD" in t -> Color(0xFFC792EA)
        "STRING" in t || "CHAR" in t -> Color(0xFFC3E88D)
        "COMMENT" in t -> Color(0xFF6B7794)
        "NUMBER" in t || "LITERAL" in t -> Color(0xFFF78C6C)
        "FUNCTION" in t || "METHOD" in t -> Color(0xFF82AAFF)
        "TYPE" in t || "CLASS" in t -> Color(0xFFFFCB6B)
        "ANNOTATION" in t || "TAG" in t || "ATTRIBUTE" in t -> Color(0xFF89DDFF)
        "OPERATOR" in t || "PUNCT" in t -> Color(0xFF89DDFF)
        else -> Color(0xFFEAEEF8)
    }
}
