package com.rexaps.rexgit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RexGitViewModel : ViewModel() {

    private val repositoryManager = RexGitRepositoryManager()
    private val pushManager = RexGitPushManager()

    private val _state = MutableStateFlow(
        RexGitUiState(isLoading = true)
    )

    val state: StateFlow<RexGitUiState> =
        _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {

            RexGitStorage.ensureDirectories()

            val repositories = repositoryManager.scan()

            _state.value = RexGitUiState(
                repositories = repositories,
                selectedRepository = null,
                isLoading = false
            )
        }
    }

    fun selectRepository(repository: RexGitRepository) {

        viewModelScope.launch(Dispatchers.IO) {

            val branch = repositoryManager.currentBranch(repository)
            val remote = repositoryManager.remote(repository)
            val files = repositoryManager.files(repository)
            val status = repositoryManager.status(repository)

            val updated = repository.copy(
                branch = branch,
                remoteUrl = remote,
                isGitRepository = true
            )

            _state.value = _state.value.copy(
                selectedRepository = updated,
                files = files,
                gitStatus = status,
                message = null,
                error = null
            )
        }
    }

    fun backToRepositories() {
        _state.value = _state.value.copy(
            selectedRepository = null,
            files = emptyList()
        )
    }

    fun refreshStatus() {

        val repository =
            _state.value.selectedRepository
                ?: return

        viewModelScope.launch(Dispatchers.IO) {

            val status = repositoryManager.status(
                repository
            )

            val files = repositoryManager.files(
                repository
            )

            _state.value = _state.value.copy(
                gitStatus = status,
                files = files
            )
        }
    }

    fun push(
        message: String,
        bumpVersion: Boolean
    ) {

        val repository =
            _state.value.selectedRepository
                ?: return

        if (_state.value.isPushing) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {

            _state.value = _state.value.copy(
                isPushing = true,
                message = "Preparing push...",
                error = null
            )

            val result = pushManager.push(
                repository = repository,
                commitMessage = message,
                bumpVersion = bumpVersion
            )

            if (result.success) {

                val status =
                    repositoryManager.status(repository)

                _state.value = _state.value.copy(
                    isPushing = false,
                    gitStatus = status,
                    message = buildString {
                        append(result.message)

                        result.version?.let {
                            append(
                                "\nVersion ${it.versionName} (${it.versionCode})"
                            )
                        }
                    },
                    error = null
                )

            } else {

                _state.value = _state.value.copy(
                    isPushing = false,
                    message = null,
                    error = result.message
                )
            }
        }
    }
}
