package com.rexaps.rextools

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language

enum class ToolCategory(val displayName: String) {
    SEARCH("Search"),
    DOWNLOAD("Download"),
    IMAGE("Image"),
    UTILITY("Utility")
}

data class RexTool(
    val id: String,
    val name: String,
    val description: String,
    val route: String,
    val category: ToolCategory,
    val icon: ImageVector = Icons.Outlined.Build,
    val isNew: Boolean = false,
    val isBeta: Boolean = false
)

data class PinterestResponse(
    val status: Boolean,
    val creator: String?,
    val result: List<String>?
)

data class PinterestUiState(
    val loading: Boolean = false,
    val images: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val query: String = "",
    val error: String? = null
)

data class SsWebResponse(
    val status: Boolean? = null,
    val creator: String? = null,
    val result: SsWebResult? = null
)

data class SsWebResult(
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val file_url: String? = null,
    val publisher: String? = null,
    val lang: String? = null,
    val screenshot: SsWebScreenshot? = null
)

data class SsWebScreenshot(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val size_pretty: String? = null
)
