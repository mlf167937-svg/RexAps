package com.rexaps.rexmusic

import kotlinx.coroutines.flow.update

/** Start the already-prepared player at the requested sync position. Must be called on main. */
    fun RexPlayerController.__split_startSyncPlayback_35(positionMs: Long) {
        val mp = player ?: return
        if (!prepared || !requestFocus()) return
        val d = _state.value.durationMs
        val target = positionMs.coerceIn(0L, d).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        seekJob?.cancel()
        seekPending = false
        if (kotlin.math.abs(_state.value.positionMs - target.toLong()) <= 25L) {
            runCatching { if (!mp.isPlaying) mp.start() }
            _state.update { it.copy(isPlaying = true) }
            return
        }
        runCatching {
            mp.setOnSeekCompleteListener { completed ->
                if (player === completed) {
                    runCatching { if (!completed.isPlaying) completed.start() }
                    _state.update { it.copy(positionMs = target.toLong(), isPlaying = true) }
                    completed.setOnSeekCompleteListener(null)
                }
            }
            mp.seekTo(target)
        }.onFailure {
            runCatching { if (!mp.isPlaying) mp.start() }
            _state.update { it.copy(isPlaying = true) }
        }
    }
