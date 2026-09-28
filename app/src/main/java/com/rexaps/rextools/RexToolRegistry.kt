package com.rexaps.rextools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.VideoLibrary

/**
 * Single source of truth for every tool available inside RexTools.
 * Add a new tool here and it automatically appears under its category
 * on the RexTools home screen — no navigation graph changes needed
 * beyond registering the composable route.
 */
object RexToolRegistry {

    const val ROUTE_PINTEREST_SEARCH = "tool_pinterest_search"
    const val ROUTE_TIKTOK_SEARCH = "tool_tiktok_search"
    const val ROUTE_SPOTIFY_SEARCH = "tool_spotify_search"
    const val ROUTE_TIKTOK_DOWNLOADER = "tool_tiktok_downloader"
    const val ROUTE_PINTEREST_DOWNLOADER = "tool_pinterest_downloader"
    const val ROUTE_YOUTUBE_DOWNLOADER = "tool_youtube_downloader"

    val tools: List<RexTool> = listOf(
        RexTool(
            id = "pinterest_search",
            name = "Pinterest Search",
            description = "Search and preview Pinterest pins",
            category = ToolCategory.SEARCH,
            icon = Icons.Outlined.Image,
            route = ROUTE_PINTEREST_SEARCH
        ),
        RexTool(
            id = "tiktok_search",
            name = "TikTok Search",
            description = "Find TikTok videos by keyword",
            category = ToolCategory.SEARCH,
            icon = Icons.Outlined.VideoLibrary,
            route = ROUTE_TIKTOK_SEARCH
        ),
        RexTool(
            id = "spotify_search",
            name = "Spotify Search",
            description = "Search tracks, albums, and artists",
            category = ToolCategory.SEARCH,
            icon = Icons.Outlined.MusicNote,
            route = ROUTE_SPOTIFY_SEARCH
        ),
        RexTool(
            id = "tiktok_downloader",
            name = "TikTok Downloader",
            description = "Save TikTok videos from a link",
            category = ToolCategory.DOWNLOADER,
            icon = Icons.Outlined.Download,
            route = ROUTE_TIKTOK_DOWNLOADER
        ),
        RexTool(
            id = "pinterest_downloader",
            name = "Pinterest Downloader",
            description = "Save Pinterest images from a link",
            category = ToolCategory.DOWNLOADER,
            icon = Icons.Outlined.Download,
            route = ROUTE_PINTEREST_DOWNLOADER
        ),
        RexTool(
            id = "youtube_downloader",
            name = "YouTube Downloader",
            description = "Save YouTube video or audio",
            category = ToolCategory.DOWNLOADER,
            icon = Icons.Outlined.PlayCircle,
            route = ROUTE_YOUTUBE_DOWNLOADER,
            isBeta = true
        )
    )

    fun toolsFor(category: ToolCategory): List<RexTool> =
        tools.filter { it.category == category }

    fun categoriesWithTools(): List<ToolCategory> =
        ToolCategory.entries.filter { toolsFor(it).isNotEmpty() }
}
