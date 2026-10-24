package com.rexaps.rexwarp.ui.components

import java.util.Locale

internal fun formatMbps(value: Double?): String = value?.takeIf { it.isFinite() && it >= 0.0 }
    ?.let { String.format(Locale.getDefault(), "%.2f Mbps", it) } ?: "—"

internal fun formatMs(value: Double?): String = value?.takeIf { it.isFinite() && it >= 0.0 }
    ?.let { String.format(Locale.getDefault(), "%.1f ms", it) } ?: "—"

internal fun formatNumber(value: Double?): String = value?.takeIf { it.isFinite() }
    ?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "—"
