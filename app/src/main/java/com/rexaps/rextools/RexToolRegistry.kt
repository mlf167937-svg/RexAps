package com.rexaps.rextools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language

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
