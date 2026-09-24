package com.rexaps.rexnux

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RexNuxViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefs =
        application.getSharedPreferences(
            "rexnux",
            Context.MODE_PRIVATE
        )

    private val installer =
        RexNuxInstaller(application)

    private val _uiState =
        MutableStateFlow(
            RexNuxUiState(
                installedDistro = loadInstalledDistro()
            )
        )

    val uiState: StateFlow<RexNuxUiState> =
        _uiState.asStateFlow()

    private fun loadInstalledDistro(): RexNuxDistro? =
        prefs.getString("installed_distro", null)
            ?.let {
                runCatching {
                    RexNuxDistro.valueOf(it)
                }.getOrNull()
            }

    fun openDownloadScreen() {
        _uiState.value = _uiState.value.copy(
            showDownloadScreen = true,
            error = null
        )
    }

    fun closeDownloadScreen() {
        _uiState.value = _uiState.value.copy(
            showDownloadScreen = false
        )
    }

    fun selectDistro(distro: RexNuxDistro) {
        _uiState.value = _uiState.value.copy(
            selectedDistro = distro,
            error = null
        )
    }

    fun startInstall() {
        val distro =
            _uiState.value.selectedDistro ?: return

        if (distro == RexNuxDistro.ARCH) {
            _uiState.value = _uiState.value.copy(
                error = "Arch Linux installer belum tersedia."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                downloading = true,
                progress = 0f,
                message = "Starting installer...",
                error = null
            )

            try {
                installer.install(distro) { progress ->
                    _uiState.value = _uiState.value.copy(
                        progress = progress.progress,
                        message = progress.message
                    )
                }

                prefs.edit()
                    .putString(
                        "installed_distro",
                        distro.name
                    )
                    .apply()

                _uiState.value = _uiState.value.copy(
                    installedDistro = distro,
                    selectedDistro = null,
                    downloading = false,
                    progress = 1f,
                    message = "Installation complete.",
                    showDownloadScreen = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    downloading = false,
                    error =
                        e.message ?: "Installation failed."
                )
            }
        }
    }

    fun openSettings() {
        _uiState.value = _uiState.value.copy(
            showSettings = true
        )
    }

    fun closeSettings() {
        _uiState.value = _uiState.value.copy(
            showSettings = false
        )
    }

    fun removeDistro() {
        prefs.edit()
            .remove("installed_distro")
            .apply()

        _uiState.value = _uiState.value.copy(
            installedDistro = null,
            selectedDistro = null,
            showSettings = false,
            showTerminal = false,
            error = null
        )
    }

    fun openRexNux() {
        if (_uiState.value.installedDistro !=
            RexNuxDistro.ALPINE
        ) {
            _uiState.value = _uiState.value.copy(
                error =
                    "Alpine Linux harus terinstall untuk membuka RexNux."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            showTerminal = true,
            error = null
        )
    }

    fun closeRexNux() {
        _uiState.value = _uiState.value.copy(
            showTerminal = false
        )
    }
}
