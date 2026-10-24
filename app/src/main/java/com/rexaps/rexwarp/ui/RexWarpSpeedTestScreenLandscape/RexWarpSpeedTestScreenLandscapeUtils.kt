package com.rexaps.rexwarp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.util.Locale

internal fun formatMbps(value: Double): String = when {
    !value.isFinite() || value < 0 -> "—"
    value >= 1000 -> String.format(Locale.US, "%.0f", value)
    value >= 100 -> String.format(Locale.US, "%.1f", value)
    else -> String.format(Locale.US, "%.2f", value)
}

internal fun formatDecimal(value: Double): String = if (!value.isFinite() || value < 0) "—" else String.format(Locale.US, "%.1f", value)
