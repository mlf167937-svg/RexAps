package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.rexaps.rexcoder.theme.Rex

// ───────────── Bottom panel: Problems / Output / Terminal ─────────────

internal val monoSmall: TextStyle
    get() = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Rex.Text)

// ───────────── Extra keys (muncul saat keyboard terbuka) ─────────────

internal val extraKeys = listOf(
    "TAB" to "    ", "{" to "{", "}" to "}", "(" to "(", ")" to ")", "[" to "[", "]" to "]",
    "<" to "<", ">" to ">", ";" to ";", ":" to ":", "\"" to "\"", "'" to "'", "=" to "=",
    "/" to "/", "\\" to "\\", "|" to "|", "&" to "&", "!" to "!", "_" to "_"
)
