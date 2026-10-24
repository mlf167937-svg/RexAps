package com.rexaps.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import com.rexaps.ui.theme.*

internal fun rexFontFamily(token: String): FontFamily = when (token.lowercase()) {
    "serif" -> FontFamily.Serif
    "monospace" -> FontFamily.Monospace
    "cursive" -> FontFamily.Cursive
    else -> FontFamily.SansSerif
}

internal fun rexFontWeight(value: Int): FontWeight = when {
    value >= 800 -> FontWeight.ExtraBold
    value >= 700 -> FontWeight.Bold
    value >= 600 -> FontWeight.SemiBold
    value >= 500 -> FontWeight.Medium
    value <= 300 -> FontWeight.Light
    else -> FontWeight.Normal
}
