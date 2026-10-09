package com.rexaps.rextools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Preview

object RexToolRegistry {

    val allTools: List<RexTool> = listOf(
            RexTool(
                id = "pinterest",
                name = "Pinterest Search",
                description = "cari & download gambar dari pinterest",
                route = "pinterest",
                category = ToolCategory.SEARCH,
                icon = Icons.Outlined.Image,
                isNew = true
            ),
            RexTool(
                id = "ssweb",
                name = "Screenshot Web",
                description = "screenshot halaman web ukuran custom",
                route = "ssweb",
                category = ToolCategory.UTILITY,
                icon = Icons.Outlined.Language,
                isNew = true
            ),
            RexTool(
                id = "tiktok_stalker",
                name = "TikTok Stalker",
                description = "lihat profil TikTok (followers, bio, stats)",
                route = "tiktok_stalker",
                category = ToolCategory.STALK,
                icon = Icons.Outlined.Person,
                isNew = true
            ),
            RexTool(
                id = "media_preview",
                name = "Media Preview",
                description = "preview gambar & video dari URL / JSON",
                route = "media_preview",
                category = ToolCategory.UTILITY,
                icon = Icons.Outlined.Preview,
                isNew = true
            ),
            RexTool(
                id = "tiktok_downloader",
                name = "TikTok Downloader",
                description = "download video & foto TikTok tanpa watermark",
                route = "tiktok_downloader",
                category = ToolCategory.DOWNLOAD,
                icon = Icons.Outlined.Download,
                isNew = true
            )
        )

    fun toolsFor(category: ToolCategory): List<RexTool> =
        allTools.filter { it.category == category }

    fun categoriesWithTools(): List<ToolCategory> =
        ToolCategory.values().filter { cat -> allTools.any { it.category == cat } }

    fun findById(id: String): RexTool? =
        allTools.firstOrNull { it.id == id }

    fun findByRoute(route: String): RexTool? =
        allTools.firstOrNull { it.route == route }
}
