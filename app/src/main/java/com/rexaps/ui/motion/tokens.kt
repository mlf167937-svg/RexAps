package com.rexaps.ui.motion

data class RexMotionTokens(
    val instant: Int = 70, val fast: Int = 145, val normal: Int = 250,
    val slow: Int = 420, val slower: Int = 640, val ambientMin: Int = 10000,
    val ambientMax: Int = 42000, val staggerMs: Int = 34, val staggerLimit: Int = 12,
    val maxParticlesFull: Int = 60, val maxParticlesBalanced: Int = 30,
    val maxParticlesSaver: Int = 12
)
val RexMotion = RexMotionTokens()
