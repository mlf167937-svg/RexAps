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
            id = "rexpanel",
            name = "RexPanel",
            description = "Monitoring dan kontrol SSH"
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
        ),
        RexModule(
            id = "rexgit",
            name = "RexGit",
            description = "Git manager & repo"
        ),
        RexModule(
            id = "rexchat",
            name = "RexChat",
            description = "WA Client - Pairing only"
        ),
        RexModule(
            id = "rextools",
            name = "RexTools",
            description = "Kumpulan tools harian"
        ),
        RexModule(
            id = "rexai",
            name = "RexAI",
            description = "AI tools & generator",
            available = false
        )
    )

    fun getModule(id: String): RexModule? {
        return modules.firstOrNull { it.id == id }
    }
}
