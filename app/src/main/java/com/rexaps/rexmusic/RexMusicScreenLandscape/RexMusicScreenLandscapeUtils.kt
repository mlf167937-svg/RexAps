package com.rexaps.rexmusic

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.*

internal val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

internal val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
