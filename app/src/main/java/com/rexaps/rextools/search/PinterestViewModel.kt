package com.rexaps.rextools.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rexaps.rextools.PinterestUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PinterestViewModel : ViewModel() {

    private val _state = MutableStateFlow(PinterestUiState())
    val state: StateFlow<PinterestUiState> = _state

    fun search(query: String) {
        if (query.isBlank()) return
        _state.value = _state.value.copy(
            loading = true,
            query = query,
            error = null
        )
        viewModelScope.launch {
            PinterestApi.search(query)
                .onSuccess { list ->
                    _state.value = _state.value.copy(
                        loading = false,
                        images = list,
                        currentIndex = 0,
                        error = null
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        loading = false,
                        error = e.message ?: "gagal fetch"
                    )
                }
        }
    }

    fun next() {
        val s = _state.value
        if (s.images.isEmpty()) return
        val nextIdx = (s.currentIndex + 1) % s.images.size
        _state.value = s.copy(currentIndex = nextIdx)
    }

    fun prev() {
        val s = _state.value
        if (s.images.isEmpty()) return
        val prevIdx = if (s.currentIndex - 1 < 0) s.images.size - 1 else s.currentIndex - 1
        _state.value = s.copy(currentIndex = prevIdx)
    }

    fun currentImage(): String? {
        val s = _state.value
        return s.images.getOrNull(s.currentIndex)
    }
}
