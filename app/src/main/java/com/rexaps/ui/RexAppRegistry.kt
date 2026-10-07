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
            description = "Browser cepat & ringan",
            available = true
        ),

        RexModule(
            id = "rexcoder",
            name = "RexCoder",
            description = "VS Code Version RexAps",
            available = true
        ),

        RexModule(
            id = "rextools",
            name = "RexTools",
            description = "Kumpulan tools harian",
            available = true
        ),

        RexModule(
            id = "rexmusic",
            name = "RexMusic",
            description = "Streaming & playlist",
            available = true
        ),

        RexModule(
            id = "rexgit",
            name = "RexGit",
            description = "Git manager & repo",
            available = true
        ),

        RexModule(
            id = "rexmonitor",
            name = "RexMonitor",
            description = "Monitoring Device",
            available = true
        ),

        RexModule(
            id = "rexmanager",
            name = "RexManager",
            description = "File manager lengkap",
            available = true
        ),

        RexModule(
            id = "rexpanel",
            name = "RexPanel",
            description = "Monitoring dan kontrol SSH",
            available = true
        ),

        RexModule(
            id = "rexchat",
            name = "RexChat",
            description = "WA Client - Pairing only",
            available = true
        ),

        RexModule(
            id = "rexnux",
            name = "RexNux",
            description = "Terminal & Linux",
            available = true
        ),

        RexModule(
            id = "rexwarp",
            name = "RexWARP",
            description = "Cloudflare WARP & network monitoring",
            available = true
        ),

        RexModule(
            id = "rexcalc",
            name = "RexCalc",
            description = "Kalkulator Pintar",
            available = false
        ),

        RexModule(
            id = "rexalbum",
            name = "RexAlbum",
            description = "Smart Galery",
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
            id = "rexai",
            name = "RexAI",
            description = "AI tools & generator",
            available = false
        )
    )

    fun getModule(id: String): RexModule? {
        return modules.firstOrNull {
            it.id.equals(id, ignoreCase = true)
        }
    }
}