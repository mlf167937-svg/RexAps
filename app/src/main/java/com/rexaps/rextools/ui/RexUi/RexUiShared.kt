package com.rexaps.rextools.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp

/* ═════════════════════ TOKENS ═════════════════════ */

val RexSm = RoundedCornerShape(12.dp)

val RexMd = RoundedCornerShape(18.dp)

val RexLg = RoundedCornerShape(26.dp)

/** Easing "ease-out expo": cepat di awal, mendarat sangat halus. */
val RexEase = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/* ── Toast ── */

@Stable
class RexToastState {
    var message by mutableStateOf<String?>(null)
        private set
    var isError by mutableStateOf(false)
        private set
    var tick by mutableStateOf(0)
        private set

    fun show(text: String, error: Boolean = false) {
        message = text
        isError = error
        tick++
    }

    fun dismiss() { message = null }
}

/* ═════════════════════ LOADING / PHASE ═════════════════════ */

enum class RexPhase { Idle, Loading, Result, Hidden }
