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
    private val prefs = application.getSharedPreferences("rexnux", Context.MODE_PRIVATE)
    private val installer = RexNuxInstaller(application)

    private val _uiState = MutableStateFlow(RexNuxUiState())
    val uiState: StateFlow<RexNuxUiState> = _uiState.asStateFlow()

    init {
        refreshState()
    }

    fun refreshState() {
        viewModelScope.launch {
            val installed = installer.getInstalledDistros()
            val sizes = installed.associateWith { installer.getFormattedDistroSize(it) }
            val lastActive = prefs.getString("active_distro", null)?.let { name ->
                runCatching { RexNuxDistro.valueOf(name) }.getOrNull()
            } ?: installed.firstOrNull()

            _uiState.value = _uiState.value.copy(
                installedDistros = installed,
                activeDistro = lastActive,
                distroSizes = sizes
            )
        }
    }

    fun openDownloadScreen(distro: RexNuxDistro? = null) {
        _uiState.value = _uiState.value.copy(
            showDownloadScreen = true,
            selectedDistro = distro,
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
        val distro = _uiState.value.selectedDistro ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                downloading = true,
                progress = 0f,
                message = "Menyiapkan instalasi ${distro.title}...",
                error = null
            )
            try {
                installer.install(distro) { prog ->
                    _uiState.value = _uiState.value.copy(
                        progress = prog.progress,
                        message = prog.message
                    )
                }
                prefs.edit().putString("active_distro", distro.name).apply()
                refreshState()
                _uiState.value = _uiState.value.copy(
                    activeDistro = distro,
                    downloading = false,
                    progress = 1f,
                    message = "Instalasi selesai.",
                    showDownloadScreen = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    downloading = false,
                    error = e.message ?: "Instalasi gagal."
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

    fun removeDistro(distro: RexNuxDistro) {
        viewModelScope.launch {
            installer.uninstall(distro)
            val updatedInstalled = installer.getInstalledDistros()
            val newActive = if (_uiState.value.activeDistro == distro) {
                updatedInstalled.firstOrNull()
            } else {
                _uiState.value.activeDistro
            }
            if (newActive != null) {
                prefs.edit().putString("active_distro", newActive.name).apply()
            } else {
                prefs.edit().remove("active_distro").apply()
            }
            refreshState()
        }
    }

    fun openTerminal(distro: RexNuxDistro) {
        if (!installer.isDistroInstalled(distro)) {
            _uiState.value = _uiState.value.copy(
                error = "${distro.title} belum terinstall. Silakan instal terlebih dahulu."
            )
            return
        }
        prefs.edit().putString("active_distro", distro.name).apply()
        _uiState.value = _uiState.value.copy(
            activeDistro = distro,
            showTerminal = true,
            error = null
        )
    }

    fun closeTerminal() {
        _uiState.value = _uiState.value.copy(
            showTerminal = false
        )
    }
}
