package com.rexaps.ui.motion

import com.rexaps.ui.theme.RexAnimationLevel

fun effectiveParticleLimit(level: RexAnimationLevel, systemReduceMotion: Boolean, followSystem: Boolean): Int =
    if (level == RexAnimationLevel.OFF || (followSystem && systemReduceMotion)) 0 else when(level) {
        RexAnimationLevel.FULL -> 60
        RexAnimationLevel.BALANCED -> 30
        RexAnimationLevel.SAVER -> 12
        RexAnimationLevel.OFF -> 0
    }
fun effectiveDuration(requestedMs: Int, level: RexAnimationLevel, systemReduceMotion: Boolean, followSystem: Boolean): Int =
    if (level == RexAnimationLevel.OFF || (followSystem && systemReduceMotion)) 0
    else if (level == RexAnimationLevel.SAVER) minOf(requestedMs, 160) else requestedMs
