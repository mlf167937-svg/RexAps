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
    private val gitDevStore = RexGitGitDevStore(application)
    private val accountPrefs = application
        .getSharedPreferences("rexgit_accounts", android.content.Context.MODE_PRIVATE)

    // Verified accounts (username resolved from the token) and which one is in use.
    @Volatile
    private var accounts: List<RexGitAuth> = emptyList()

    @Volatile
    private var activeIndex: Int = 0

    private val _state = MutableStateFlow(RexGitUiState(isLoading = true))
    val state: StateFlow<RexGitUiState> = _state.asStateFlow()

    // Editor text lives here so typing doesn't recompose the whole screen.
    @Volatile
    private var editorBuffer: String = ""

    companion object {
        private const val MAX_EDITOR_BYTES = 400_000L

        private const val ACTIVE_KEY = "active_username"
        private const val REPO_ACCOUNT_PREFIX = "repo_account:"
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

    private fun missingAuthError(): IllegalStateException {
        return IllegalStateException(
            "No GitHub account connected.\n\n" +
                "Open the GitHub dialog and log in with your username and token, " +
                "or create:\n" +
                authStore.tokenPath()
        )
    }

    private fun publishAccounts() {
        val list = accounts
        val index = activeIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))
        activeIndex = index

        _state.update {
            it.copy(
                accounts = list.map { auth -> auth.username },
                activeAccount = index,
                githubUsername = list.getOrNull(index)?.username,
                isGithubConnected = list.isNotEmpty()
            )
        }
    }

    /**
     * Reads every account in token.env and asks GitHub which username each
     * token belongs to. Accounts with a bad token are skipped; if EVERY token
     * fails, the first error is thrown like before.
     */
    private fun reloadAccounts(): List<String> {
        val entries = authStore.loadAccounts()

        val resolved = mutableListOf<RexGitAuth>()
        val skipped = mutableListOf<String>()
        var firstError: Throwable? = null

        entries.forEachIndexed { index, entry ->
            try {
                resolved += RexGitAuth(
                    username = github.getUser(entry.token),
                    token = entry.token
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                if (firstError == null) firstError = t
                skipped += entry.username ?: "account ${index + 1}"
            }
        }

        val error = firstError
        if (resolved.isEmpty() && error != null) throw error

        accounts = resolved

        val saved = accountPrefs.getString(ACTIVE_KEY, null)
        activeIndex = resolved
            .indexOfFirst { it.username.equals(saved, ignoreCase = true) }
            .takeIf { it >= 0 }
            ?: activeIndex.coerceIn(0, (resolved.size - 1).coerceAtLeast(0))

        publishAccounts()

        return skipped
    }

    /**
     * Returns the account currently in use.
     * [reload] re-reads token.env (used by refresh / connect).
     */
    private suspend fun externalGithubAuth(reload: Boolean = false): RexGitAuth? {
        if (reload || accounts.isEmpty()) {
            val skipped = reloadAccounts()

            if (skipped.isNotEmpty()) {
                _state.update {
                    it.copy(message = "Skipped invalid token: ${skipped.joinToString()}")
                }
            }
        }

        return accounts.getOrNull(activeIndex)
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
                    val auth = externalGithubAuth(reload = true)

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
                                accounts = emptyList(),
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
                val savedAccount = accountPrefs.getString(REPO_ACCOUNT_PREFIX + repository.path, null)
                val desiredAccount = savedAccount ?: repository.accountNickname
                val desiredIndex = accounts.indexOfFirst { it.username.equals(desiredAccount, ignoreCase = true) }
                if (desiredIndex >= 0 && desiredIndex != activeIndex) {
                    activeIndex = desiredIndex
                    accountPrefs.edit().putString(ACTIVE_KEY, accounts[desiredIndex].username).apply()
                    publishAccounts()
                }
                val selectedAccountName = accounts.getOrNull(activeIndex)?.username
                if (selectedAccountName != null) {
                    accountPrefs.edit().putString(REPO_ACCOUNT_PREFIX + repository.path, selectedAccountName).apply()
                }

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
                        gitDev = gitDevStore.load(repository.path),
                        readmeContent = readReadme(repository.path),
                        workflows = emptyList(),
                        workflowRuns = emptyList(),
                        workflowArtifacts = emptyList(),
                        releases = emptyList(),
                        isRepoHubLoading = true,
                        repoHubError = null,
                        message = null
                    )
                }
                loadRepositoryGithubData(repository.copy(branch = branch, remoteUrl = remote, isGitRepository = isGit))
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
                gitDev = RexGitGitDevConfig(),
                readmeContent = "",
                workflows = emptyList(),
                workflowRuns = emptyList(),
                workflowArtifacts = emptyList(),
                releases = emptyList(),
                isRepoHubLoading = false,
                repoHubError = null,
                downloadingArtifact = null,
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
                    readmeContent = readReadme(repo.path),
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

    /**
     * Latest editor text. The UI reads it when the layout switches between
     * portrait and landscape so unsaved typing is never lost.
     */
    fun editorText(): String = editorBuffer

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
                        readmeContent = if (File(path).name.equals("README.md", true)) content else it.readmeContent,
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

    private fun readReadme(repositoryPath: String): String {
        return try {
            val root = File(repositoryPath)
            val readme = root.listFiles()?.firstOrNull { it.isFile && it.name.equals("README.md", true) }
                ?: return ""
            if (readme.length() > MAX_EDITOR_BYTES) return "README is too large to preview in RexGit. Open the file from Code to inspect it."
            readme.readText(Charsets.UTF_8)
        } catch (_: Throwable) { "" }
    }

    private fun repositorySlug(repo: RexGitRepository): Pair<String, String>? {
        val remote = repo.remoteUrl?.trim().orEmpty()
        val match = Regex("""(?:github\.com[:/])([^/]+)/([^/]+?)(?:\.git)?/?$""", RegexOption.IGNORE_CASE).find(remote)
        if (match != null) return match.groupValues[1] to match.groupValues[2]
        val globalOwner = repo.globalOwner
        if (!globalOwner.isNullOrBlank()) return globalOwner to repo.name
        val account = repo.accountNickname
        if (!account.isNullOrBlank()) return account to repo.name
        return null
    }

    private fun loadRepositoryGithubData(repo: RexGitRepository) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val slug = repositorySlug(repo)
                if (slug == null) {
                    _state.update { it.copy(isRepoHubLoading = false, repoHubError = "No GitHub remote is configured for this repository.") }
                    return@launch
                }
                val auth = try { externalGithubAuth() } catch (_: Throwable) { null }
                val snapshot = try { github.loadActions(auth?.token, slug.first, slug.second) } catch (t: Throwable) {
                    RexGitActionsSnapshot(emptyList(), emptyList(), emptyList())
                }
                val releaseData = try { github.listReleases(auth?.token, slug.first, slug.second) } catch (_: Throwable) { emptyList() }
                _state.update { cur ->
                    if (cur.selectedRepository?.path != repo.path) cur else cur.copy(
                        workflows = snapshot.workflows,
                        workflowRuns = snapshot.runs,
                        workflowArtifacts = snapshot.artifacts,
                        releases = releaseData,
                        isRepoHubLoading = false,
                        repoHubError = if (snapshot.workflows.isEmpty() && snapshot.runs.isEmpty() && releaseData.isEmpty()) {
                            "No GitHub Actions or releases were found, or this token cannot access them."
                        } else null
                    )
                }
            } catch (t: Throwable) {
                _state.update { cur -> if (cur.selectedRepository?.path != repo.path) cur else cur.copy(isRepoHubLoading = false, repoHubError = t.readableMessage("Could not load GitHub details")) }
            }
        }
    }

    fun refreshRepositoryHub() {
        val repo = _state.value.selectedRepository ?: return
        _state.update { it.copy(isRepoHubLoading = true, repoHubError = null) }
        loadRepositoryGithubData(repo)
    }

    fun downloadGithubAsset(url: String, rawName: String) {
        if (_state.value.selectedRepository == null) return
        val name = rawName.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_").take(120).ifBlank { "github-download.zip" }
        launchIO {
            _state.update { it.copy(downloadingArtifact = name, repoHubError = null) }
            try {
                val auth = try { externalGithubAuth() } catch (_: Throwable) { null }
                val downloadDir = RexGitStorage.downloadsDirectory()
                val requested = File(downloadDir, name)
                val output = if (!requested.exists()) requested else {
                    val base = requested.nameWithoutExtension
                    val extension = requested.extension.takeIf { it.isNotBlank() }?.let { ".$it" }.orEmpty()
                    var index = 2
                    var candidate = File(downloadDir, "$base-$index$extension")
                    while (candidate.exists()) { index++; candidate = File(downloadDir, "$base-$index$extension") }
                    candidate
                }
                val accept = if (url.contains("/releases/assets/")) "application/octet-stream" else "application/vnd.github+json"
                github.download(url, auth?.token, output, accept)
                _state.update { it.copy(message = "Downloaded $name to ${output.parentFile?.absolutePath}") }
            } catch (t: Throwable) {
                _state.update { it.copy(repoHubError = t.readableMessage("Download failed")) }
            } finally {
                _state.update { it.copy(downloadingArtifact = null) }
            }
        }
    }

    // ---------- GitHub ----------

    /**
     * Loads the account(s) from token.env. The parameter is ignored; it stays
     * so older callers keep compiling. To log in with a typed username and
     * token use [addAccount].
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
                val auth = externalGithubAuth(reload = true)
                    ?: throw missingAuthError()

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
        accounts = emptyList()

        _state.update {
            it.copy(
                githubUsername = null,
                isGithubConnected = false,
                accounts = emptyList(),
                activeAccount = 0,
                githubRepositories = emptyList(),
                message = "GitHub disconnected"
            )
        }
    }

    /**
     * Logs in with a typed username + token, checks the token with GitHub and
     * saves it to token.env (slot 1, or slot 2 when an account already exists).
     */
    fun addAccount(username: String, token: String) {
        launchIO {
            _state.update {
                it.copy(
                    isGithubLoading = true,
                    error = null,
                    message = null
                )
            }

            try {
                val typedName = username.trim().removePrefix("@")
                val typedToken = token.trim()

                if (typedName.isEmpty() || typedToken.isEmpty()) {
                    throw IllegalArgumentException("Enter both your GitHub username and token.")
                }

                val raw = authStore.loadAccounts()

                if (raw.size >= RexGitAuthStore.MAX_ACCOUNTS) {
                    throw IllegalStateException(
                        "You can connect up to ${RexGitAuthStore.MAX_ACCOUNTS} accounts. " +
                            "Remove one first."
                    )
                }

                if (raw.any { it.token == typedToken }) {
                    throw IllegalStateException("This token is already added.")
                }

                val login = github.getUser(typedToken)

                if (!login.equals(typedName, ignoreCase = true)) {
                    throw IllegalStateException(
                        "This token belongs to @$login, not @$typedName.\n" +
                            "Check the username or use another token."
                    )
                }

                if (accounts.any { it.username.equals(login, ignoreCase = true) }) {
                    throw IllegalStateException("@$login is already connected.")
                }

                // Fill in usernames we already know so token.env stays readable.
                val known = accounts.associate { it.token to it.username }
                val merged = raw.map { it.copy(username = it.username ?: known[it.token]) } +
                    RexGitEnvAccount(login, typedToken)

                authStore.saveAccounts(merged)

                accountPrefs.edit().putString(ACTIVE_KEY, login).apply()

                val auth = externalGithubAuth(reload = true)
                    ?: throw missingAuthError()

                _state.update {
                    it.copy(message = "Connected as @${auth.username}")
                }

                fetchGithubRepositories(auth)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                fail(
                    "Could not add the account:\n" +
                        t.readableMessage("Invalid or expired token")
                )
            } finally {
                _state.update {
                    it.copy(isGithubLoading = false)
                }
            }
        }
    }

    /** Switches which connected account is used for the GitHub list, clone, pull and push. */
    fun switchAccount(index: Int) {
        val list = accounts

        if (index !in list.indices || index == activeIndex) return

        activeIndex = index
        accountPrefs.edit().putString(ACTIVE_KEY, list[index].username).apply()
        _state.value.selectedRepository?.let { repo ->
            accountPrefs.edit().putString(REPO_ACCOUNT_PREFIX + repo.path, list[index].username).apply()
        }

        _state.update {
            it.copy(
                githubRepositories = emptyList(),
                githubSearchMode = false,
                githubSearchQuery = "",
                githubSearchResults = emptyList()
            )
        }

        publishAccounts()

        launchIO {
            _state.update {
                it.copy(message = "Using @${list[index].username}")
            }

            fetchGithubRepositories(list[index])
        }
    }

    /** Removes an account from token.env. */
    fun removeAccount(index: Int) {
        val target = accounts.getOrNull(index) ?: return

        launchIO {
            _state.update {
                it.copy(isGithubLoading = true, error = null)
            }

            try {
                val remaining = authStore.loadAccounts()
                    .filterNot { it.token == target.token }

                authStore.saveAccounts(remaining)

                accounts = accounts.filterNot { it.token == target.token }
                activeIndex = 0

                accounts.firstOrNull()?.let {
                    accountPrefs.edit().putString(ACTIVE_KEY, it.username).apply()
                }

                publishAccounts()

                _state.update {
                    it.copy(
                        githubRepositories = emptyList(),
                        message = "Removed @${target.username}"
                    )
                }

                accounts.getOrNull(activeIndex)?.let { fetchGithubRepositories(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                fail(t.readableMessage("Could not update token.env"))
            } finally {
                _state.update {
                    it.copy(isGithubLoading = false)
                }
            }
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

    fun searchGithubRepositories(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) {
            _state.update { it.copy(githubSearchMode = false, githubSearchQuery = "", githubSearchResults = emptyList()) }
            return
        }
        launchIO {
            _state.update { it.copy(isGithubLoading = true, githubSearchMode = true, repoHubError = null) }
            try {
                val auth = externalGithubAuth() ?: throw missingAuthError()
                val results = github.searchRepositories(auth.token, clean)
                _state.update {
                    it.copy(
                        githubSearchMode = true,
                        githubSearchQuery = clean,
                        githubSearchResults = results,
                        message = "Found ${results.size} repositories for \"$clean\""
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                fail("GitHub repository search failed:\n" + t.readableMessage("Search failed"))
            } finally {
                _state.update { it.copy(isGithubLoading = false) }
            }
        }
    }

    fun reloadGithub() {
        launchIO {
            try {
                val auth = externalGithubAuth(reload = true)

                if (auth == null) {
                    fail(missingAuthError().message ?: "No GitHub account connected.")
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
                    ?: throw missingAuthError()

                val upstreamOwner = repository.fullName.substringBefore('/')
                val result = repositoryManager.clone(
                    url = repository.cloneUrl,
                    name = repository.name,
                    username = auth.username,
                    token = auth.token,
                    accountNickname = auth.username,
                    globalOwner = upstreamOwner.takeIf { !it.equals(auth.username, ignoreCase = true) }
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
                    ?: throw missingAuthError()

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
        commitFolder: String = "",
        useGitDev: Boolean = false
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
                    pushStage = "Checking Files...",
                    pushDetail = "Preparing RexPush",
                    pushLog = emptyList(),
                    pushFinished = false,
                    pushSuccess = false,
                    pushError = null,
                    pushCommitMessage = null,
                    pushVersion = null,
                    error = null,
                    message = null
                )
            }

            try {
                val auth = externalGithubAuth()
                    ?: throw missingAuthError()

                val result = pushManager.push(
                    repository = repo,
                    commitMessage = message,
                    bumpVersion = bumpVersion,
                    commitType = commitType,
                    commitFolder = commitFolder,
                    gitDev = current.gitDev,
                    useGitDev = useGitDev,
                    username = auth.username,
                    token = auth.token,
                    onProgress = { stage, detail, logLine ->
                        _state.update { cur ->
                            cur.copy(
                                pushStage = stage,
                                pushDetail = detail,
                                pushLog = if (logLine.isNullOrBlank()) {
                                    cur.pushLog
                                } else {
                                    (cur.pushLog + logLine).takeLast(80)
                                }
                            )
                        }
                    }
                )

                _state.update {
                    it.copy(
                        pushStage = if (result.success) "RexPush" else "Push failed",
                        pushDetail = if (result.success) "Successfully pushed" else result.message,
                        pushFinished = true,
                        pushSuccess = result.success,
                        pushError = result.message.takeIf { !result.success },
                        pushVersion = result.version,
                        pushCommitMessage = if (result.success && message.isNotBlank()) {
                            if (useGitDev) RexGitCommitMeta.buildMessage(
                                version = result.version,
                                type = commitType,
                                folder = commitFolder,
                                description = message
                            ) else RexGitCommitMeta.buildStandardMessage(commitType, message)
                        } else null
                    )
                }

                if (result.success) {
                    _state.update {
                        it.copy(pushStage = "Refreshing", pushDetail = "Updating repository status")
                    }
                    reloadRepository(repo)
                    _state.update {
                        it.copy(
                            pushStage = "RexPush",
                            pushDetail = "Successfully pushed",
                            pushFinished = true,
                            pushSuccess = true
                        )
                    }
                }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        pushStage = "Push failed",
                        pushDetail = t.readableMessage("Push failed"),
                        pushFinished = true,
                        pushSuccess = false,
                        pushError = t.readableMessage("Push failed")
                    )
                }
            } finally {
                _state.update {
                    it.copy(isPushing = false)
                }
            }
        }
    }

    // ---------- GitDev ----------

    /**
     * Saves GitDev mode for the selected repository. Turning it ON needs a
     * package path that really contains folders (e.g. com/rexaps).
     */
    fun saveGitDev(config: RexGitGitDevConfig) {
        val repo = _state.value.selectedRepository
            ?: return

        val clean = RexGitGitDev.normalize(config)

        if (clean.enabled) {
            if (clean.packagePath.isBlank()) {
                fail("Fill in the package path first, for example: com/rexaps")
                return
            }

            val found = RexGitGitDev.detectFolders(File(repo.path), clean)

            if (found.isEmpty()) {
                fail(
                    "No folders found under ${clean.packagePath}.\n" +
                        "Check the package path (and module) in the GitDev guide."
                )
                return
            }
        }

        gitDevStore.save(repo.path, clean)

        _state.update {
            it.copy(
                gitDev = clean,
                message = if (clean.enabled) "GitDev metadata is ready for this repository."
                else "GitDev is off. Standard Push remains available."
            )
        }
    }


    fun clearPushState() {
        _state.update {
            it.copy(
                pushStage = "Checking Files...",
                pushDetail = "",
                pushLog = emptyList(),
                pushFinished = false,
                pushSuccess = false,
                pushError = null,
                pushCommitMessage = null,
                pushVersion = null
            )
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
