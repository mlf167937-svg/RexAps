package com.rexaps.rexmusic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel tipis. Semua logika player ada di [RexPlayerController] (hidup di level aplikasi)
 * supaya musik + notifikasi tetap jalan walau layar ditutup.
 */
class RexMusicViewModel(app: Application) : AndroidViewModel(app) {

    init {
        RexPlayerController.init(app)
    }

    val state: StateFlow<RexMusicUiState> = RexPlayerController.state

    fun search(query: String) = RexPlayerController.search(query)
    fun clearSearch() = RexPlayerController.clearSearch()
    fun dismissError() = RexPlayerController.dismissError()
    fun clearHistory() = RexPlayerController.clearHistory()

    fun playFromMain(index: Int) = RexPlayerController.playFromMain(index)
    fun playFromSearch(index: Int) = RexPlayerController.playFromSearch(index)
    fun playFromHistory(index: Int) = RexPlayerController.playFromHistory(index)

    fun next() = RexPlayerController.next()
    fun prev(force: Boolean = false) = RexPlayerController.prev(force)
    fun togglePlay() = RexPlayerController.togglePlay()
    fun seekTo(ratio: Float) = RexPlayerController.seekTo(ratio)

    fun currentTrack(): RexTrack? = RexPlayerController.currentTrack()
}
