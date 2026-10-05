package com.rexaps.rexgit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class RexGitViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repositoryManager = RexGitRepositoryManager()
    private val pushManager = RexGitPushManager()
    private val github = RexGitGithubApi()
    private val authStore = RexGitAuthStore(application)

    private val _state = MutableStateFlow(RexGitUiState(isLoading = true))
    val state: StateFlow<RexGitUiState> = _state.asStateFlow()

    // Editor text lives here so typing doesn't recompose the whole screen.
    @Volatile
    private var editorBuffer: String = ""

    companion object {
        private const val MAX_EDITOR_BYTES = 400_000L

        private const val TOKEN_FILE_PATH =
            "/sdcard/Download/RexAps/Github/token.env"
    }

    init {
        RexGitStorage.init(application)
        RexGitJGit.init(application)
        loadInitial()
    }

    // ---------- helpers ----------

    /** Runs on IO and converts ANY throwable (including Errors) into a UI error instead of a crash. */
    private fun launchIO(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                fail(t.readableMessage("Unexpected error"))
            }
        }
    }

    private fun fail(text: String) {
        _state.update {
            it.copy(
                error = text,
                message = null
            )
        }
    }

    private fun storageInfo(): Pair<String, Boolean> {
        return RexGitStorage.githubRoot.absolutePath to RexGitStorage.usingFallback
    }

    /**
     * Reads the GitHub token from:
     *
     * /sdcard/Download/RexAps/Github/token.env
     *
     * Example:
     *
     * token="ghp_anuanu"
     *
     * Also accepts:
     *
     * token='ghp_anuanu'
     * token=ghp_anuanu
     */
    private fun externalGithubToken(): String? {
        val file = File(TOKEN_FILE_PATH)

        if (!file.isFile) {
            return null
        }

        return try {
            val content = file.readText(Charsets.UTF_8)

            val regex = Regex(
                """(?m)^\s*token\s*=\s*["']?([^"'\r\n]+)["']?\s*$"""
            )

            regex.find(content)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Loads the token from token.env and asks GitHub
     * for the username belonging to that token.
     */
    private suspend fun externalGithubAuth(): RexGitAuth? {
        val token = externalGithubToken()
            ?: return null

        val username = github.getUser(token)

        return RexGitAuth(
            username = username,
            token = token
        )
    }

    // ---------- home ----------

    private fun loadInitial() {
        launchIO {
            try {
                RexGitStorage.ensureDirectories()

                val auth = try {
                    externalGithubAuth()
                } catch (_: Throwable) {
                    null
                }

                val repos = repositoryManager.scan()
                val (path, fallback) = storageInfo()

                _state.update {
                    it.copy(
                        repositories = repos,
                        githubUsername = auth?.username,
                        isGithubConnected = auth != null,
                        storagePath = path,
                        storageFallback = fallback
                    )
                }

                if (auth != null) {
                    fetchGithubRepositories(auth)
                }
            } finally {
                _state.update {
                    it.copy(isLoading = false)
                }
            }
        }
    }

    fun refresh() {
        launchIO {
            _state.update {
                it.copy(isLoading = true)
            }

            try {
                val repos = repositoryManager.scan()
                val (path, fallback) = storageInfo()

                _state.update {
                    it.copy(
                        repositories = repos,
                        storagePath = path,
                        storageFallback = fallback
                    )
                }

                try {
                    val auth = externalGithubAuth()

                    if (auth != null) {
                        _state.update {
                            it.copy(
                                githubUsername = auth.username,
                                isGithubConnected = true
                            )
                        }

                        fetchGithubRepositories(auth)
                    } else {
                        _state.update {
                            it.copy(
                                githubUsername = null,
                                isGithubConnected = false,
                                githubRepositories = emptyList()
                            )
                        }
                    }
                } catch (t: Throwable) {
                    fail(
                        "Could not connect to GitHub:\n" +
                            t.readableMessage("Invalid or expired token")
                    )
                }
            } finally {
                _state.update {
                    it.copy(isLoading = false)
                }
            }
        }
    }

    // ---------- repository ----------

    fun selectRepository(repository: RexGitRepository) {
        if (_state.value.isRepoLoading) return

        launchIO {
            _state.update {
                it.copy(
                    isRepoLoading = true,
                    error = null
                )
            }

            try {
                val isGit = File(repository.path, ".git").exists()

                val branch =
                    if (isGit) {
                        repositoryManager.currentBranch(repository)
                    } else {
                        ""
                    }

                val remote =
                    if (isGit) {
                        repositoryManager.remote(repository)
                    } else {
                        null
                    }

                val files = repositoryManager.files(repository)

                val status =
                    if (isGit) {
                        repositoryManager.status(repository)
                    } else {
                        RexGitStatus()
                    }

                editorBuffer = ""

                _state.update {
                    it.copy(
                        selectedRepository = repository.copy(
                            branch = branch,
                            remoteUrl = remote,
                            isGitRepository = isGit
                        ),
                        files = files,
                        currentDirectory = null,
                        gitStatus = status,
                        editorPath = null,
                        editorContent = "",
                        editorDirty = false,
                        message = null
                    )
                }
            } finally {
                _state.update {
                    it.copy(isRepoLoading = false)
                }
            }
        }
    }

    fun backToRepositories() {
        editorBuffer = ""

        _state.update {
            it.copy(
                selectedRepository = null,
                files = emptyList(),
                currentDirectory = null,
                gitStatus = RexGitStatus(),
                editorPath = null,
                editorContent = "",
                editorDirty = false
            )
        }
    }

    private fun reloadRepository(repo: RexGitRepository) {
        val dir = _state.value.currentDirectory
            ?.takeIf { File(it).isDirectory }

        val status =
            if (repo.isGitRepository) {
                repositoryManager.status(repo)
            } else {
                RexGitStatus()
            }

        val branch =
            if (repo.isGitRepository) {
                repositoryManager.currentBranch(repo)
            } else {
                repo.branch
            }

        val files = repositoryManager.files(repo, dir)

        _state.update { cur ->
            val selected = cur.selectedRepository

            if (selected == null || selected.path != repo.path) {
                cur
            } else {
                cur.copy(
                    gitStatus = status,
                    files = files,
                    currentDirectory = dir,
                    selectedRepository = selected.copy(
                        branch = branch
                    )
                )
            }
        }
    }

    fun refreshStatus() {
        val repo = _state.value.selectedRepository
            ?: return

        launchIO {
            _state.update {
                it.copy(isRepoLoading = true)
            }

            try {
                reloadRepository(repo)
            } finally {
                _state.update {
                    it.copy(isRepoLoading = false)
                }
            }
        }
    }

    // ---------- files ----------

    fun openDirectory(directory: String) {
        val repo = _state.value.selectedRepository
            ?: return

        launchIO {
            val files = repositoryManager.files(
                repo,
                directory
            )

            _state.update {
                it.copy(
                    files = files,
                    currentDirectory = directory
                )
            }
        }
    }

    fun openRoot() {
        val repo = _state.value.selectedRepository
            ?: return

        launchIO {
            val files = repositoryManager.files(repo)

            _state.update {
                it.copy(
                    files = files,
                    currentDirectory = null
                )
            }
        }
    }

    fun openParent() {
        val repo = _state.value.selectedRepository
            ?: return

        val current = _state.value.currentDirectory
            ?: return

        val parent = File(current).parentFile

        if (parent == null || parent.absolutePath == repo.path) {
            openRoot()
        } else {
            openDirectory(parent.absolutePath)
        }
    }

    fun openFile(file: RexGitFile) {
        if (file.isDirectory) {
            openDirectory(file.path)
            return
        }

        launchIO {
            val target = File(file.path)

            if (target.length() > MAX_EDITOR_BYTES) {
                fail(
                    "This file is too large to edit on a phone " +
                        "(limit 400 KB)."
                )
                return@launchIO
            }

            if (looksBinary(target)) {
                fail("Binary files can't be opened in the editor.")
                return@launchIO
            }

            val content = target.readText()

            editorBuffer = content

            _state.update {
                it.copy(
                    editorPath = file.path,
                    editorContent = content,
                    editorDirty = false,
                    error = null
                )
            }
        }
    }

    private fun looksBinary(file: File): Boolean {
        return file.inputStream().use { stream ->
            val buffer = ByteArray(8000)
            val read = stream.read(buffer)

            if (read <= 0) {
                false
            } else {
                (0 until read).any {
                    buffer[it] == 0.toByte()
                }
            }
        }
    }

    // ---------- create / delete ----------

    fun createFile(name: String) {
        val repo = _state.value.selectedRepository
            ?: return

        val dir = _state.value.currentDirectory
        val clean = name.trim()

        launchIO {
            _state.update {
                it.copy(
                    isRepoLoading = true,
                    error = null
                )
            }

            try {
                val result = repositoryManager.createFile(
                    repo,
                    dir,
                    clean
                )

                if (result.success) {
                    reloadRepository(repo)

                    _state.update {
                        it.copy(message = result.message)
                    }

                    openFile(
                        RexGitFile(
                            name = clean,
                            path = File(
                                dir ?: repo.path,
                                clean
                            ).absolutePath,
                            isDirectory = false
                        )
                    )
                } else {
                    fail(result.message)
                }
            } finally {
                _state.update {
                    it.copy(isRepoLoading = false)
                }
            }
        }
    }

    fun createFolder(name: String) {
        val repo = _state.value.selectedRepository
            ?: return

        val dir = _state.value.currentDirectory
        val clean = name.trim()

        launchIO {
            _state.update {
                it.copy(
                    isRepoLoading = true,
                    error = null
                )
            }

            try {
                val result = repositoryManager.createFolder(
                    repo,
                    dir,
                    clean
                )

                if (result.success) {
                    reloadRepository(repo)

                    _state.update {
                        it.copy(message = result.message)
                    }
                } else {
                    fail(result.message)
                }
            } finally {
                _state.update {
                    it.copy(isRepoLoading = false)
                }
            }
        }
    }

    fun deleteEntry(file: RexGitFile) {
        val repo = _state.value.selectedRepository
            ?: return

        launchIO {
            _state.update {
                it.copy(
                    isRepoLoading = true,
                    error = null
                )
            }

            try {
                val result = repositoryManager.delete(
                    repo,
                    file.path
                )

                reloadRepository(repo)

                if (result.success) {
                    _state.update {
                        it.copy(message = result.message)
                    }
                } else {
                    fail(result.message)
                }
            } finally {
                _state.update {
                    it.copy(isRepoLoading = false)
                }
            }
        }
    }

    // ---------- editor ----------

    fun updateEditor(content: String) {
        editorBuffer = content

        _state.update {
            if (it.editorDirty) {
                it
            } else {
                it.copy(editorDirty = true)
            }
        }
    }

    fun closeEditor() {
        editorBuffer = ""

        _state.update {
            it.copy(
                editorPath = null,
                editorContent = "",
                editorDirty = false
            )
        }
    }

    fun saveEditor() {
        val path = _state.value.editorPath
            ?: return

        if (_state.value.isSaving) return

        val content = editorBuffer

        launchIO {
            _state.update {
                it.copy(isSaving = true)
            }

            try {
                File(path).writeText(content)

                val repo = _state.value.selectedRepository

                val status =
                    if (repo != null && repo.isGitRepository) {
                        repositoryManager.status(repo)
                    } else {
                        null
                    }

                _state.update {
                    it.copy(
                        editorDirty = editorBuffer != content,
                        gitStatus = status ?: it.gitStatus,
                        message = "File saved"
                    )
                }
            } finally {
                _state.update {
                    it.copy(isSaving = false)
                }
            }
        }
    }

    // ---------- GitHub ----------

    /**
     * The token parameter is intentionally ignored.
     *
     * This parameter remains here so the current RexGitScreen.kt
     * can still call:
     *
     * viewModel.connectGithub(...)
     *
     * The real token always comes from token.env.
     */
    fun connectGithub(
        @Suppress("UNUSED_PARAMETER")
        tokenFromUi: String
    ) {
        launchIO {
            _state.update {
                it.copy(
                    isGithubLoading = true,
                    error = null,
                    message = null
                )
            }

            try {
                val auth = externalGithubAuth()
                    ?: throw IllegalStateException(
                        "GitHub token tidak ditemukan.\n\n" +
                            "Buat file:\n" +
                            TOKEN_FILE_PATH
                    )

                _state.update {
                    it.copy(
                        githubUsername = auth.username,
                        isGithubConnected = true,
                        message = "Connected as @${auth.username}"
                    )
                }

                fetchGithubRepositories(auth)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                fail(
                    "Could not connect to GitHub:\n" +
                        t.readableMessage(
                            "Invalid or expired token"
                        )
                )
            } finally {
                _state.update {
                    it.copy(isGithubLoading = false)
                }
            }
        }
    }

    fun disconnectGithub() {
        /*
         * IMPORTANT:
         *
         * We do NOT delete token.env.
         *
         * The token is managed externally.
         */
        _state.update {
            it.copy(
                githubUsername = null,
                isGithubConnected = false,
                githubRepositories = emptyList(),
                message = "GitHub disconnected"
            )
        }
    }

    private fun fetchGithubRepositories(
        auth: RexGitAuth
    ) {
        _state.update {
            it.copy(isGithubLoading = true)
        }

        try {
            val repos = github.listRepositories(
                auth.token
            )

            _state.update {
                it.copy(
                    githubRepositories = repos
                )
            }
        } catch (t: Throwable) {
            if (t is CancellationException) {
                throw t
            }

            fail(
                "Could not load GitHub repositories:\n" +
                    t.readableMessage("Network error")
            )
        } finally {
            _state.update {
                it.copy(isGithubLoading = false)
            }
        }
    }

    fun reloadGithub() {
        launchIO {
            try {
                val auth = externalGithubAuth()

                if (auth == null) {
                    fail(
                        "GitHub token tidak ditemukan.\n\n" +
                            TOKEN_FILE_PATH
                    )
                    return@launchIO
                }

                _state.update {
                    it.copy(
                        githubUsername = auth.username,
                        isGithubConnected = true
                    )
                }

                fetchGithubRepositories(auth)
            } catch (t: Throwable) {
                if (t is CancellationException) {
                    throw t
                }

                fail(
                    "Could not load GitHub repositories:\n" +
                        t.readableMessage("Network error")
                )
            }
        }
    }

    fun cloneRepository(
        repository: RexGitGithubRepository
    ) {
        if (_state.value.isCloning) return

        launchIO {
            _state.update {
                it.copy(
                    cloningName = repository.name,
                    error = null
                )
            }

            try {
                val auth = externalGithubAuth()
                    ?: throw IllegalStateException(
                        "GitHub token tidak ditemukan.\n\n" +
                            TOKEN_FILE_PATH
                    )

                val result = repositoryManager.clone(
                    url = repository.cloneUrl,
                    name = repository.name,
                    username = auth.username,
                    token = auth.token
                )

                if (!result.success) {
                    fail(result.message)
                } else {
                    val repos = repositoryManager.scan()

                    _state.update {
                        it.copy(
                            repositories = repos,
                            message = result.message
                        )
                    }
                }
            } finally {
                _state.update {
                    it.copy(cloningName = null)
                }
            }
        }
    }

    // ---------- pull / push ----------

    fun pull() {
        val repo = _state.value.selectedRepository
            ?: return

        val current = _state.value

        if (
            !repo.isGitRepository ||
            current.isPulling ||
            current.isPushing
        ) {
            return
        }

        launchIO {
            _state.update {
                it.copy(
                    isPulling = true,
                    error = null,
                    message = null
                )
            }

            try {
                val auth = externalGithubAuth()
                    ?: throw IllegalStateException(
                        "GitHub token tidak ditemukan.\n\n" +
                            TOKEN_FILE_PATH
                    )

                val result = repositoryManager.pull(
                    repo,
                    auth.username,
                    auth.token
                )

                reloadRepository(repo)

                if (result.success) {
                    _state.update {
                        it.copy(message = result.message)
                    }
                } else {
                    _state.update {
                        it.copy(error = result.message)
                    }
                }
            } finally {
                _state.update {
                    it.copy(isPulling = false)
                }
            }
        }
    }

    fun push(
        message: String,
        bumpVersion: Boolean,
        commitType: RexGitCommitType = RexGitCommitType.FIX,
        commitFolder: String = ""
    ) {
        val repo = _state.value.selectedRepository
            ?: return

        val current = _state.value

        if (
            !repo.isGitRepository ||
            current.isPushing ||
            current.isPulling
        ) {
            return
        }

        launchIO {
            _state.update {
                it.copy(
                    isPushing = true,
                    error = null,
                    message = null
                )
            }

            try {
                val auth = externalGithubAuth()
                    ?: throw IllegalStateException(
                        "GitHub token tidak ditemukan.\n\n" +
                            TOKEN_FILE_PATH
                    )

                val result = pushManager.push(
                    repository = repo,
                    commitMessage = message,
                    bumpVersion = bumpVersion,
                    commitType = commitType,
                    commitFolder = commitFolder,
                    username = auth.username,
                    token = auth.token
                )

                reloadRepository(repo)

                if (result.success) {
                    val text = buildString {
                        append(result.message)

                        result.version?.let {
                            append(
                                "\nVersion " +
                                    "${it.versionName} " +
                                    "(${it.versionCode})"
                            )
                        }
                    }

                    _state.update {
                        it.copy(message = text)
                    }
                } else {
                    _state.update {
                        it.copy(error = result.message)
                    }
                }
            } finally {
                _state.update {
                    it.copy(isPushing = false)
                }
            }
        }
    }

    // ---------- messages ----------

    fun dismissMessage() {
        _state.update {
            it.copy(message = null)
        }
    }

    fun dismissError() {
        _state.update {
            it.copy(error = null)
        }
    }

    fun clearMessage() {
        _state.update {
            it.copy(
                message = null,
                error = null
            )
        }
    }
}
