package com.rexaps.ui

enum class RexRoute(
    val moduleId: String?,
    val displayName: String
) {
    NONE(null, ""),

    REXFOX(
        "rexfox",
        "RexFox"
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
    );

    companion object {

        fun fromModuleId(id: String): RexRoute? {
            return entries.firstOrNull {
                it.moduleId.equals(id, ignoreCase = true)
            }
        }
    }
}