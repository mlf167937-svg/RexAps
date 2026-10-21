package com.rexaps.ui.motion

data class RexMotionTokens(
    val instant: Int = 80, val fast: Int = 150, val normal: Int = 250,
    val slow: Int = 400, val slower: Int = 600, val ambientMin: Int = 8000,
    val ambientMax: Int = 40000, val staggerMs: Int = 40, val staggerLimit: Int = 10,
    val maxParticlesFull: Int = 60, val maxParticlesBalanced: Int = 30,
    val maxParticlesSaver: Int = 12
)
val RexMotion = RexMotionTokens()
