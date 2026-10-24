package com.rexaps.rexmusic

import android.os.Build
import kotlinx.coroutines.flow.update

internal fun RexPlayerController.__split_abandonFocus_38() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
    }

/**
     * loading = search / resolve / prepare.
     * Buffering (lag saat memutar) hanya lewat [LoadPhase] supaya halaman browse tidak ikut loading.
     */
    internal fun RexPlayerController.__split_publishLoading_39() = _state.update {
        val phase = when {
            resolving -> LoadPhase.Resolving
            preparing -> LoadPhase.Preparing
            buffering -> LoadPhase.Buffering
            searching -> LoadPhase.Searching
            else -> LoadPhase.Idle
        }
        it.copy(
            loading = searching || resolving || preparing,
            phase = phase,
            loadingText = when (phase) {
                LoadPhase.Resolving -> resolveText
                LoadPhase.Preparing -> "menyiapkan audio..."
                LoadPhase.Buffering -> "buffering..."
                LoadPhase.Searching -> "mencari..."
                LoadPhase.Idle -> ""
            }
        )
    }
