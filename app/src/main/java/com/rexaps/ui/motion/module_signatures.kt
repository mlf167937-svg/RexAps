package com.rexaps.ui.motion

/** Stable, app-owned motion identity for every discovered module. */
object RexModuleSignatures {
    val byModule: Map<String, String> = linkedMapOf(
        "rexwarp" to "portal tunnel rotation; accelerate on connect; wave ring on success",
        "rexcoder" to "glowing caret; execution progress sweep; typewriter output",
        "rexgit" to "branch stroke-draw; pulsing HEAD node; flowing push/pull particles",
        "rexarchive" to "archive stack compresses into a sealed package",
        "rexcode" to "syntax token highlight sweep and language glyph pulse",
        "rexfox" to "fox-tail orbit around the active tool icon",
        "rexmanager" to "service cards arrange into a responsive constellation",
        "rexnux" to "network nodes connect with a soft signal pulse",
        "rexspacex" to "launch trajectory draws upward with a restrained exhaust trail",
        "rextools" to "tool glyph rotates into place on selection",
        "rexpanel" to "panel edge light sweeps when opened",
        "rexadm" to "secure shield outline draws on authorization success",
        "rexmonitor" to "telemetry trace scrolls with a moving endpoint",
        "rexmusic" to "waveform bars breathe to playback state",
        "rexchat" to "message bubbles fade-slide with a subtle typing indicator"
    )
    fun signature(moduleId: String): String = byModule[moduleId.lowercase()] ?: "fade-slide entrance, shimmer loading, illustrated empty state, semantic error and success feedback"
}
