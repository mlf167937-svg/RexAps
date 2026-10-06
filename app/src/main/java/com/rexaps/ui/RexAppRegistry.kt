package com.rexaps.ui

data class RexModule(
    val id: String,
    val name: String,
    val description: String,
    val available: Boolean = true
)

object RexAppRegistry {

    val modules = listOf(
        RexModule("rexfox",     "RexFox",     "Browser cepat & ringan",                true),
        RexModule("rextools",   "RexTools",   "Kumpulan tools harian",                 true),
        RexModule("rexmusic",   "RexMusic",   "Streaming & playlist",                  true),
        RexModule("rexgit",     "RexGit",     "Git manager & repo",                    true),
        RexModule("rexmonitor", "RexMonitor", "Monitoring Device",                   true),
        RexModule("rexmanager", "RexManager", "File manager lengkap",                  true),
        RexModule("rexpanel",   "RexPanel",   "Monitoring dan kontrol SSH",            true),
        RexModule("rexchat",    "RexChat",    "WA Client - Pairing only",              true),
        RexModule("rexnux",     "RexNux",     "Terminal & Linux",                      true),
        RexModule("rexwarp",    "RexWARP",    "Cloudflare WARP & network monitoring",  true),
        RexModule("rexcalc",    "RexCalc",    "Kalkulator Pintar",                     false),
        RexModule("rexalbum",   "RexAlbum",   "Smart Galery",                          false),
        RexModule("rextube",    "RexTube",    "Video & subscriptions",                 false),
        RexModule("rextok",     "RexTok",     "Short video & feed",                    false),
        RexModule("rexai",      "RexAI",      "AI tools & generator",                  false)
    )

    fun getModule(id: String): RexModule? =
        modules.firstOrNull { it.id == id }
}