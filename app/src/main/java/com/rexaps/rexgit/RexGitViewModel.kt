package com.rexaps.rexgit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class RexGitViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repositoryManager =
        RexGitRepositoryManager()

    private val pushManager =
        RexGitPushManager()

    private val github =
        RexGitGithubApi()

    private val authStore =
        RexGitAuthStore(application)

    private val _state =
        MutableStateFlow(
            RexGitUiState(
                isLoading = true
            )
        )

    val state: StateFlow<RexGitUiState> =
        _state.asStateFlow()

    init {
        loadInitial()
    }

    private fun loadInitial() {

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            RexGitStorage.ensureDirectories()

            val auth =
                authStore.load()

            val repos =
                repositoryManager.scan()

            _state.value =
                _state.value.copy(
                    repositories = repos,
                    githubUsername =
                        auth?.username,
                    isGithubConnected =
                        auth != null,
                    isLoading = false
                )

            if (auth != null) {
                loadGithubRepositories(auth)
            }
        }
    }

    fun refresh() {

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            val repos =
                repositoryManager.scan()

            _state.value =
                _state.value.copy(
                    repositories = repos
                )

            _state.value.selectedRepository
                ?.let {
                    selectRepository(it)
                }
        }
    }

    fun selectRepository(
        repository: RexGitRepository
    ) {

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            val branch =
                repositoryManager.currentBranch(
                    repository
                )

            val remote =
                repositoryManager.remote(
                    repository
                )

            val files =
                repositoryManager.files(
                    repository
                )

            val status =
                repositoryManager.status(
                    repository
                )

            val updated =
                repository.copy(
                    branch = branch,
                    remoteUrl = remote,
                    isGitRepository = true
                )

            _state.value =
                _state.value.copy(
                    selectedRepository = updated,
                    files = files,
                    currentDirectory = null,
                    gitStatus = status,
                    editorPath = null,
                    editorContent = "",
                    editorDirty = false,
                    message = null,
                    error = null
                )
        }
    }

    fun backToRepositories() {

        _state.value =
            _state.value.copy(
                selectedRepository = null,
                files = emptyList(),
                currentDirectory = null,
                editorPath = null,
                editorContent = "",
                editorDirty = false
            )
    }

    fun openDirectory(
        directory: String
    ) {

        val repo =
            _state.value.selectedRepository
                ?: return

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            val files =
                repositoryManager.files(
                    repo,
                    directory
                )

            _state.value =
                _state.value.copy(
                    files = files,
                    currentDirectory = directory
                )
        }
    }

    fun openRoot() {

        val repo =
            _state.value.selectedRepository
                ?: return

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            _state.value =
                _state.value.copy(
                    files =
                        repositoryManager.files(repo),
                    currentDirectory = null
                )
        }
    }

    fun openFile(
        file: RexGitFile
    ) {

        if (file.isDirectory) {
            openDirectory(file.path)
            return
        }

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            try {

                val target =
                    File(file.path)

                if (target.length() > 2_000_000L) {
                    _state.value =
                        _state.value.copy(
                            error =
                                "File terlalu besar untuk editor"
                        )
                    return@launch
                }

                val content =
                    target.readText()

                _state.value =
                    _state.value.copy(
                        editorPath = file.path,
                        editorContent = content,
                        editorDirty = false,
                        error = null
                    )

            } catch (e: Exception) {

                _state.value =
                    _state.value.copy(
                        error =
                            e.message
                                ?: "File gagal dibuka"
                    )
            }
        }
    }

    fun updateEditor(
        content: String
    ) {

        _state.value =
            _state.value.copy(
                editorContent = content,
                editorDirty = true
            )
    }

    fun closeEditor() {

        _state.value =
            _state.value.copy(
                editorPath = null,
                editorContent = "",
                editorDirty = false
            )
    }

    fun saveEditor() {

        val path =
            _state.value.editorPath
                ?: return

        val content =
            _state.value.editorContent

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            try {

                _state.value =
                    _state.value.copy(
                        isSaving = true
                    )

                File(path).writeText(content)

                _state.value =
                    _state.value.copy(
                        isSaving = false,
                        editorDirty = false,
                        message = "File berhasil disimpan"
                    )

                refreshStatus()

            } catch (e: Exception) {

                _state.value =
                    _state.value.copy(
                        isSaving = false,
                        error =
                            e.message
                                ?: "Gagal menyimpan file"
                    )
            }
        }
    }

    fun refreshStatus() {

        val repo =
            _state.value.selectedRepository
                ?: return

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            val status =
                repositoryManager.status(repo)

            _state.value =
                _state.value.copy(
                    gitStatus = status
                )
        }
    }

    fun connectGithub(
        username: String,
        token: String
    ) {

        if (
            username.isBlank() ||
            token.isBlank()
        ) {
            _state.value =
                _state.value.copy(
                    error =
                        "Username dan token wajib diisi"
                )
            return
        }

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            try {

                _state.value =
                    _state.value.copy(
                        isGithubLoading = true,
                        error = null,
                        message = "Menghubungkan GitHub..."
                    )

                val realUsername =
                    github.getUser(token)

                authStore.save(
                    realUsername,
                    token
                )

                _state.value =
                    _state.value.copy(
                        githubUsername =
                            realUsername,
                        isGithubConnected = true,
                        isGithubLoading = false,
                        message =
                            "GitHub terhubung sebagai @$realUsername"
                    )

                loadGithubRepositories(
                    RexGitAuth(
                        realUsername,
                        token
                    )
                )

            } catch (e: Exception) {

                _state.value =
                    _state.value.copy(
                        isGithubLoading = false,
                        error =
                            "GitHub gagal terhubung:\n" +
                                (e.message
                                    ?: "Token tidak valid")
                    )
            }
        }
    }

    fun disconnectGithub() {

        authStore.clear()

        _state.value =
            _state.value.copy(
                githubUsername = null,
                isGithubConnected = false,
                githubRepositories = emptyList(),
                message = "GitHub diputus"
            )
    }

    private fun loadGithubRepositories(
        auth: RexGitAuth
    ) {

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            try {

                _state.value =
                    _state.value.copy(
                        isGithubLoading = true
                    )

                val repos =
                    github.listRepositories(
                        auth.token
                    )

                _state.value =
                    _state.value.copy(
                        githubRepositories = repos,
                        isGithubLoading = false
                    )

            } catch (e: Exception) {

                _state.value =
                    _state.value.copy(
                        isGithubLoading = false,
                        error =
                            e.message
                                ?: "Gagal mengambil repository"
                    )
            }
        }
    }

    fun reloadGithub() {

        val auth =
            authStore.load()
                ?: return

        loadGithubRepositories(auth)
    }

    fun cloneRepository(
        repository: RexGitGithubRepository
    ) {

        val auth =
            authStore.load()

        if (auth == null) {
            _state.value =
                _state.value.copy(
                    error =
                        "Hubungkan GitHub terlebih dahulu"
                )
            return
        }

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            try {

                _state.value =
                    _state.value.copy(
                        isCloning = true,
                        error = null,
                        message =
                            "Cloning ${repository.name}..."
                    )

                val result =
                    repositoryManager.clone(
                        url = repository.cloneUrl,
                        name = repository.name,
                        username = auth.username,
                        token = auth.token
                    )

                if (!result.success) {
                    _state.value =
                        _state.value.copy(
                            isCloning = false,
                            error = result.message,
                            message = null
                        )
                    return@launch
                }

                val repos =
                    repositoryManager.scan()

                _state.value =
                    _state.value.copy(
                        repositories = repos,
                        isCloning = false,
                        message = result.message
                    )

            } catch (e: Exception) {

                _state.value =
                    _state.value.copy(
                        isCloning = false,
                        error =
                            e.message
                                ?: "Clone gagal"
                    )
            }
        }
    }

    fun pull() {

        val repo =
            _state.value.selectedRepository
                ?: return

        val auth =
            authStore.load()

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            val result =
                repositoryManager.pull(
                    repo,
                    auth?.username,
                    auth?.token
                )

            if (result.success) {

                refreshStatus()

                _state.value =
                    _state.value.copy(
                        message = result.message
                    )

            } else {

                _state.value =
                    _state.value.copy(
                        error = result.message
                    )
            }
        }
    }

    fun push(
        message: String,
        bumpVersion: Boolean
    ) {

        val repo =
            _state.value.selectedRepository
                ?: return

        if (_state.value.isPushing) {
            return
        }

        val auth =
            authStore.load()

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            _state.value =
                _state.value.copy(
                    isPushing = true,
                    message = "Preparing push...",
                    error = null
                )

            val result =
                pushManager.push(
                    repository = repo,
                    commitMessage = message,
                    bumpVersion = bumpVersion,
                    username = auth?.username,
                    token = auth?.token
                )

            if (result.success) {

                val status =
                    repositoryManager.status(
                        repo
                    )

                _state.value =
                    _state.value.copy(
                        isPushing = false,
                        gitStatus = status,
                        message = buildString {
                            append(result.message)

                            result.version?.let {
                                append(
                                    "\nVersion " +
                                        it.versionName +
                                        " (" +
                                        it.versionCode +
                                        ")"
                                )
                            }
                        },
                        error = null
                    )

            } else {

                _state.value =
                    _state.value.copy(
                        isPushing = false,
                        message = null,
                        error = result.message
                    )
            }
        }
    }

    fun clearMessage() {

        _state.value =
            _state.value.copy(
                message = null,
                error = null
            )
    }
}
