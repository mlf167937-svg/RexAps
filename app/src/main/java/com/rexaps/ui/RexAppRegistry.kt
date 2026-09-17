package com.rexaps.ui

data class RexModule(
    val id: String,
    val name: String,
    val description: String,
    val available: Boolean = true
)

object RexAppRegistry {

    val modules = listOf(
        RexModule(
            id = "rexfox",
            name = "RexFox",
            description = "Browser cepat & ringan"
        ),
        RexModule(
            id = "rexnux",
            name = "RexNux",
            description = "Terminal & Linux",
            available = false
        ),
        RexModule(
            id = "rexmusic",
            name = "RexMusic",
            description = "Musik & playlist",
            available = false
        ),
        RexModule(
            id = "rextube",
            name = "RexTube",
            description = "Video & subscriptions",
            available = false
        ),
        RexModule(
            id = "rextok",
            name = "RexTok",
            description = "Short video & feed",
            available = false
        )
    )

    fun getModule(id: String): RexModule? {
        return modules.firstOrNull { it.id == id }
    }
}
