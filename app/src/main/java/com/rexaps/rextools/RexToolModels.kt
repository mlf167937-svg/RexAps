package com.rexaps.rextools

import androidx.compose.ui.graphics.vector.ImageVector

enum class ToolCategory(val displayName: String) {
    SEARCH("Search"),
    DOWNLOADER("Downloader"),
    UTILITIES("Utilities")
}

data class RexTool(
    val id: String,
    val name: String,
    val description: String,
    val category: ToolCategory,
    val icon: ImageVector,
    val route: String,
    val isNew: Boolean = false,
    val isBeta: Boolean = false
)
