package com.rexaps.rextools.tiktok

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rexaps.rextools.TiktokDownloadUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TiktokDownloaderViewModel : ViewModel() {

    private val _state = MutableStateFlow(TiktokDownloadUiState())
    val state: StateFlow<TiktokDownloadUiState> = _state.asStateFlow()

    fun onUrlChange(v: String) {
        _state.value = _state.value.copy(url = v)
    }

    fun download() {
        val s = _state.value
        if (s.url.isBlank()) {
            _state.value = s.copy(error = "url kosong")
            return
        }
        _state.value = s.copy(loading = true, error = null, result = null)
        viewModelScope.launch {
            TiktokApi.download(s.url)
                .onSuccess { r ->
                    _state.value = _state.value.copy(loading = false, result = r)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        loading = false,
                        error = e.message ?: "gagal download"
                    )
                }
        }
    }
}
