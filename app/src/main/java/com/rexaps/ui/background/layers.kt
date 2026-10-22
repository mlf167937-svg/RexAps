package com.rexaps.ui.background

/** Category-specific visual recipes used by the ambient renderer and theme previews. */
object RexBackgroundLayers {
    val recipes: Map<String, List<String>> = mapOf(
        "Glass" to listOf("translucent depth", "slow specular reflection sweep", "soft bokeh", "prismatic edge shimmer"),
        "Dark" to listOf("brushed low-light contours", "breathing corner vignette", "fine metallic dust", "restrained accent halo"),
        "Light" to listOf("pearl-gradient mesh", "floating soft highlights", "airy surface bloom", "gentle organic drift"),
        "Neon" to listOf("perspective scan grid", "moving scanline", "electric edge pulse", "accent tube glow"),
        "Cosmic" to listOf("distant star field", "slow orbital rings", "nebula mid-layer", "rare parallax sparkle"),
        "Elemental" to listOf("layered wave ribbons", "theme-colored energy particles", "crystal / flame / lightning motif", "directional ambient glow")
    )
}
