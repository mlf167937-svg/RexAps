package com.rexaps.rextools.preview

enum class MediaType { IMAGE, VIDEO, UNKNOWN }

data class PreviewItem(
    val url: String,
    val type: MediaType,
    val id: String = url
) {
    val fileName: String
        get() = url.substringBefore('?').substringAfterLast('/').ifBlank { "media_${hashCode()}" }
}

enum class PreviewMode { INPUT, GRID, SINGLE }

data class PreviewUiState(
    val rawInput: String = "",
    val items: List<PreviewItem> = emptyList(),
    val currentIndex: Int = 0,
    val mode: PreviewMode = PreviewMode.INPUT,
    val error: String? = null
) {
    val currentItem: PreviewItem?
        get() = items.getOrNull(currentIndex)
    val hasItems: Boolean
        get() = items.isNotEmpty()
}
