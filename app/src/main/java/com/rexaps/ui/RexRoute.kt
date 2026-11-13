package com.rexaps.ui

enum class RexRoute(
    val moduleId: String?,
    val displayName: String
) {
    NONE(null, ""),

    REXLIFE(
        "rexlife",
        "RexLife"
    ),

    REXFOX(
        "rexfox",
        "RexFox"
    ),

    REXADM(
        "rexadm",
        "RexADM"
    ),

    REXPANEL(
        "rexpanel",
        "RexPanel"
    ),

    REXCHAT(
        "rexchat",
        "RexChat"
    ),

    REXNUX(
        "rexnux",
        "RexNux"
    ),

    REXTOOLS(
        "rextools",
        "RexTools"
    ),

    REXMUSIC(
        "rexmusic",
        "RexMusic"
    ),

    REXGAMES(
        "rexgames",
        "RexGames"
    ),

    REXMANAGER(
        "rexmanager",
        "RexManager"
    ),

    REXGIT(
        "rexgit",
        "RexGit"
    ),

    REXCODER(
        "rexcoder",
        "RexCoder"
    ),

    REXWARP(
        "rexwarp",
        "RexWARP"
    ),

    REXMONITOR(
        "rexmonitor",
        "RexMonitor"
    ),

    REXSPACEX(
        "rexspacex",
        "RexSpaceX"
    ),

    REXAI(
        "rexai",
        "RexAI"
    );

    companion object {

        fun fromModuleId(id: String): RexRoute? {
            return entries.firstOrNull {
                it.moduleId.equals(id, ignoreCase = true)
            }
        }
    }
}