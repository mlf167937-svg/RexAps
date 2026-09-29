package com.rexaps.rextools.ssweb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SsWebViewModel : ViewModel() {

    private val _state = MutableStateFlow(SsWebUiState())
    val state: StateFlow<SsWebUiState> = _state.asStateFlow()

    fun onUrlChange(url: String) {
        _state.value = _state.value.copy(url = url)
    }

    fun onWidthChange(w: String) {
        w.toIntOrNull()?.let { _state.value = _state.value.copy(width = it) }
    }

    fun onHeightChange(h: String) {
        h.toIntOrNull()?.let { _state.value = _state.value.copy(height = it) }
    }

    fun onScaleChange(s: String) {
        s.toIntOrNull()?.let { _state.value = _state.value.copy(scale = it) }
    }

    fun capture() {
        val s = _state.value
        val target = normalizeUrl(s.url)
        if (target.isBlank()) {
            _state.value = s.copy(error = "URL kosong")
            return
        }

        _state.value = s.copy(loading = true, error = null, result = null)
        viewModelScope.launch {
            SsWebApi.capture(target, s.width, s.height, s.scale)
                .onSuccess { r ->
                    _state.value = _state.value.copy(loading = false, result = r)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        loading = false,
                        error = e.message ?: "gagal capture"
                    )
                }
        }
    }

    private fun normalizeUrl(raw: String): String {
        val t = raw.trim()
        if (t.isBlank()) return ""
        return if (t.startsWith("http://") || t.startsWith("https://")) t
        else "https://$t"
    }
}
