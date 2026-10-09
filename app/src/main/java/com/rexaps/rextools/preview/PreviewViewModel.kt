package com.rexaps.rextools.preview

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreviewViewModel : ViewModel() {

    private val _state = MutableStateFlow(PreviewUiState())
    val state: StateFlow<PreviewUiState> = _state.asStateFlow()

    fun onInputChange(v: String) {
        _state.value = _state.value.copy(rawInput = v, error = null)
    }

    fun extract() {
        val input = _state.value.rawInput
        val parsed = PreviewParser.parse(input)
        if (parsed.isEmpty()) {
            _state.value = _state.value.copy(
                error = "tidak ada link http/https yang ditemukan",
                items = emptyList(),
                mode = PreviewMode.INPUT
            )
            return
        }
        _state.value = _state.value.copy(
            items = parsed,
            currentIndex = 0,
            mode = PreviewMode.GRID,
            error = null
        )
    }

    fun openSingle(index: Int) {
        if (index !in _state.value.items.indices) return
        _state.value = _state.value.copy(currentIndex = index, mode = PreviewMode.SINGLE)
    }

    fun backToGrid() {
        _state.value = _state.value.copy(mode = PreviewMode.GRID)
    }

    fun backToInput() {
        _state.value = _state.value.copy(mode = PreviewMode.INPUT)
    }

    fun next() {
        val s = _state.value
        if (s.items.isEmpty()) return
        val nextIdx = (s.currentIndex + 1) % s.items.size
        _state.value = s.copy(currentIndex = nextIdx)
    }

    fun prev() {
        val s = _state.value
        if (s.items.isEmpty()) return
        val prevIdx = if (s.currentIndex - 1 < 0) s.items.size - 1 else s.currentIndex - 1
        _state.value = s.copy(currentIndex = prevIdx)
    }

    fun clear() {
        _state.value = PreviewUiState()
    }
}
