package com.rexaps.rextools.tiktok

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rexaps.rextools.TiktokStalkUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TiktokStalkerViewModel : ViewModel() {

    private val _state = MutableStateFlow(TiktokStalkUiState())
    val state: StateFlow<TiktokStalkUiState> = _state.asStateFlow()

    fun onUsernameChange(v: String) {
        _state.value = _state.value.copy(username = v)
    }

    fun stalk() {
        val s = _state.value
        if (s.username.isBlank()) {
            _state.value = s.copy(error = "username kosong")
            return
        }
        _state.value = s.copy(loading = true, error = null, result = null)
        viewModelScope.launch {
            TiktokApi.stalk(s.username)
                .onSuccess { r ->
                    _state.value = _state.value.copy(loading = false, result = r)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        loading = false,
                        error = e.message ?: "gagal stalk"
                    )
                }
        }
    }
}
