package com.rexaps.rexpanel

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

internal val TermFg = Color(0xFFCDD6F4)

internal val Good = Color(0xFFA6E3A1)

internal val Warn = Color(0xFFF9E2AF)

internal val Bad = Color(0xFFF38BA8)

internal enum class PanelTab(
    val label: String,
    val icon: ImageVector
) {
    Dashboard("Dasbor", Icons.Default.Dashboard),
    Terminal("Terminal", Icons.Default.Terminal),
    Files("File", Icons.Default.Folder)
}
