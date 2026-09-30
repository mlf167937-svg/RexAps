package com.rexaps.rexmanager.utils

import android.os.SystemClock

/** Converts byte counters into throttled 0..1 fractions so the UI isn't flooded. */
class ProgressTracker(
    private val totalBytes: Long = 0L,
    private val onUpdate: (Float) -> Unit
) {
    private var done = 0L
    private var lastEmit = 0L
    private var lastFraction = -1f

    fun add(bytes: Long) {
        done += bytes
        update(done, totalBytes)
    }

    fun update(processed: Long, total: Long) {
        if (total <= 0L) return
        val fraction = (processed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        val now = SystemClock.elapsedRealtime()
        if (fraction >= 1f || (now - lastEmit >= 100L && fraction != lastFraction)) {
            lastEmit = now
            lastFraction = fraction
            onUpdate(fraction)
        }
    }
}
