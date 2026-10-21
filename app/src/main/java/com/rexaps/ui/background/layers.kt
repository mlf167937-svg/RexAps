package com.rexaps.ui.background

/** Category-specific layer recipes consumed by RexAmbientBackground and preview cards. */
object RexBackgroundLayers {
    val recipes: Map<String, List<String>> = mapOf(
        "Glass" to listOf("moon glow", "moving bokeh", "gradient fog", "glass edge shimmer"),
        "Dark" to listOf("soft dust", "breathing vignette", "controlled corner glow"),
        "Light" to listOf("pastel mesh", "floating organic shapes", "pearl highlights"),
        "Neon" to listOf("perspective grid", "scanline", "neon pulse"),
        "Cosmic" to listOf("far stars", "nebula mid-layer", "near parallax stars", "rare meteor"),
        "Elemental" to listOf("elemental glow", "ambient particles", "crystal/fire/lightning/water motif")
    )
}
