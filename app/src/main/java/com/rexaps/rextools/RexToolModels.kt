package com.rexaps.rextools

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Preview

enum class ToolCategory(val displayName: String) {
    SEARCH("Search"),
    DOWNLOAD("Download"),
    IMAGE("Image"),
    UTILITY("Utility"),
    STALK("Stalk")
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

// ───────────────────────── TikTok Stalker ─────────────────────────

data class TiktokStalkResponse(
    val status: Boolean? = null,
    val result: TiktokStalkResult? = null
)

data class TiktokStalkResult(
    val id: String? = null,
    val username: String? = null,
    val name: String? = null,
    val title: String? = null,
    val description: String? = null,
    val avatar: String? = null,
    val bio: String? = null,
    val link: String? = null,
    val verified: String? = null,
    val region: String? = null,
    val private: String? = null,
    val seller: String? = null,
    val organization: String? = null,
    val story: String? = null,
    val stats: TiktokStalkStats? = null
)

data class TiktokStalkStats(
    val followers: String? = null,
    val following: String? = null,
    val likes: String? = null,
    val videos: String? = null,
    val friend: String? = null,
    val raw_followers: Long? = null,
    val raw_following: Long? = null,
    val raw_likes: Long? = null,
    val raw_videos: Long? = null,
    val raw_friend: Long? = null
)

data class TiktokStalkUiState(
    val loading: Boolean = false,
    val username: String = "",
    val result: TiktokStalkResult? = null,
    val error: String? = null
)

// ───────────────────────── TikTok Downloader ─────────────────────────

data class TiktokDownloadResponse(
    val status: Boolean? = null,
    val result: TiktokDownloadResult? = null
)

data class TiktokDownloadResult(
    val title: String? = null,
    val taken_at: String? = null,
    val region: String? = null,
    val id: String? = null,
    val duration: String? = null,
    val cover: String? = null,
    val size_wm: String? = null,
    val size_nowm: String? = null,
    val size_nowm_hd: String? = null,
    val data: List<String>? = null,
    val music_info: TiktokMusicInfo? = null,
    val stats: TiktokDownloadStats? = null,
    val author: TiktokAuthor? = null
) {
    /** true = video (durasi > 0), false = foto */
    val isVideo: Boolean get() = duration?.let {
        val num = it.split(" ").firstOrNull()?.toFloatOrNull() ?: 0f
        num > 0f
    } ?: true
}

data class TiktokMusicInfo(
    val id: String? = null,
    val title: String? = null,
    val author: String? = null,
    val album: String? = null,
    val duration: String? = null,
    val original: String? = null,
    val copyright: String? = null,
    val url: String? = null
)

data class TiktokDownloadStats(
    val views: String? = null,
    val likes: String? = null,
    val comment: String? = null,
    val share: String? = null,
    val save: String? = null,
    val download: String? = null
)

data class TiktokAuthor(
    val id: String? = null,
    val fullname: String? = null,
    val nickname: String? = null,
    val avatar: String? = null
)

data class TiktokDownloadUiState(
    val loading: Boolean = false,
    val url: String = "",
    val result: TiktokDownloadResult? = null,
    val error: String? = null
)

// ───────────────────────── Media Preview ─────────────────────────

enum class PreviewMediaType { IMAGE, VIDEO, UNKNOWN }

data class PreviewMediaItem(
    val url: String,
    val type: PreviewMediaType,
    val id: String = url
)

enum class PreviewMode { INPUT, GRID, SINGLE }

data class PreviewUiState(
    val rawInput: String = "",
    val items: List<PreviewMediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val mode: PreviewMode = PreviewMode.INPUT,
    val error: String? = null
)
