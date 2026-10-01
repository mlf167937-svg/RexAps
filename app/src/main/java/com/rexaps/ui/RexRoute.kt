package com.rexaps.ui

enum class RexRoute(
    val moduleId: String?,
    val displayName: String
) {
    NONE(null, ""),
    REXFOX("rexfox", "RexFox"),
    REXPANEL("rexpanel", "RexPanel"),
    REXCHAT("rexchat", "RexChat"),
    REXNUX("rexnux", "RexNux"),
    REXTOOLS("rextools", "RexTools"),
    REXMUSIC("rexmusic", "RexMusic"),
    REXMANAGER("rexmanager", "RexManager");

    companion object {
        fun fromModuleId(id: String): RexRoute? =
            entries.firstOrNull { it.moduleId == id }
    }
}
