package com.rexaps.rextools.tiktok

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rexaps.rextools.TiktokFullResult
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

    // 1. STALK BIASA (NEXRAY)
    fun stalkBasic() {
        val s = _state.value
        if (s.username.isBlank()) {
            _state.value = s.copy(error = "username kosong")
            return
        }
        _state.value = s.copy(loading = true, loadingText = "Mencari via API...", error = null, result = null, fullResult = null)
        viewModelScope.launch {
            TiktokApi.stalkBasic(s.username)
                .onSuccess { r ->
                    _state.value = _state.value.copy(loading = false, result = r)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(loading = false, error = e.message ?: "gagal stalk")
                }
        }
    }

    // 2. STALK FULL (WEBVIEW SCRAPER)
    fun onFullResultReceived(json: String) {
        try {
            // Parsing JSON manual tanpa library tambahan (karena formatnya dalem banget)
            // Kita cuma butuh string JSON-nya buat di-extract pake Regex/String manipulation
            
            // 1. Ambil Bio
            val bioRegex = """"signature":"(.*?)"""".toRegex()
            val bio = bioRegex.find(json)?.groupValues?.get(1)?.replace("\\n", "\n") ?: "-"
            
            // 2. Ambil Nama
            val nameRegex = """"nickname":"(.*?)"""".toRegex()
            val name = nameRegex.find(json)?.groupValues?.get(1) ?: "Unknown"
            
            // 3. Ambil Avatar
            val avatarRegex = """"avatarLarger":"(.*?)"""".toRegex()
            val avatar = avatarRegex.find(json)?.groupValues?.get(1)?.replace("\\u002F", "/") ?: ""

            // 4. Cari Video & Story URLs
            val videoUrls = mutableMapOf<String, String>()
            val storyUrls = mutableMapOf<String, String>()
            
            // Regex buat nyari URL video mp4
            val mp4Regex = """"playAddr":"(.*?)"""".toRegex()
            val allVideos = mp4Regex.findAll(json).map { it.groupValues[1].replace("\\u002F", "/") }.toList()
            
            allVideos.forEachIndexed { index, url ->
                // Cek apakah URL ini bagian dari story (biasanya ada di objek story)
                if (json.contains(""""story":"""") && json.indexOf(url) > json.indexOf(""""story":"""")) {
                    storyUrls["story-${storyUrls.size + 1}"] = url
                } else {
                    videoUrls["video-${videoUrls.size + 1}"] = url
                }
            }

            val result = TiktokFullResult(
                name = name,
                username = _state.value.username,
                bio = bio,
                avatar = avatar,
                videos = videoUrls,
                stories = storyUrls,
                rawJson = json
            )

            _state.value = _state.value.copy(loading = false, fullResult = result)
        } catch (e: Exception) {
            _state.value = _state.value.copy(loading = false, error = "Gagal parse data: ${e.message}")
        }
    }
}
