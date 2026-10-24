package com.rexaps.rexmusic

import kotlinx.coroutines.flow.update

internal fun RexPlayerController.__splitExtra_offlineMode_1(): Boolean = RexOfflineManager.state.value.enabled

internal fun RexPlayerController.__splitExtra_onOfflineChanged_2(o: OfflineState) {
        val modeChanged = _state.value.offline.enabled != o.enabled
        _state.update { it.copy(offline = o) }
        if (modeChanged) publishQueueInfo()
    }

internal fun RexPlayerController.__splitExtra_setOfflineMode_3(on: Boolean) {
        RexOfflineManager.setOfflineMode(on)
        clearSearch()
        postNotice(if (on) "Mode offline aktif. Hanya lagu unduhan yang diputar" else "Kembali online")
    }
