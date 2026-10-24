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

    internal val repositoryManager = RexGitRepositoryManager()
    internal val pushManager = RexGitPushManager()
    internal val github = RexGitGithubApi()
    internal val authStore = RexGitAuthStore(application)
    internal val gitDevStore = RexGitGitDevStore(application)
    internal val accountPrefs = application
        .getSharedPreferences("rexgit_accounts", android.content.Context.MODE_PRIVATE)

    // Verified accounts (username resolved from the token) and which one is in use.
    @Volatile
    internal var accounts: List<RexGitAuth> = emptyList()

    @Volatile
    internal var activeIndex: Int = 0

    internal val _state = MutableStateFlow(RexGitUiState(isLoading = true))
    val state: StateFlow<RexGitUiState> = _state.asStateFlow()

    // Editor text lives here so typing doesn't recompose the whole screen.
    @Volatile
    internal var editorBuffer: String = ""

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
    internal fun launchIO(block: suspend () -> Unit) {
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

    internal fun fail(text: String) {
        _state.update {
            it.copy(
                error = text,
                message = null
            )
        }
    }

    internal fun storageInfo(): Pair<String, Boolean> {
        return RexGitStorage.githubRoot.absolutePath to RexGitStorage.usingFallback
    }

    internal fun missingAuthError(): IllegalStateException {
        return IllegalStateException(
            "No GitHub account connected.\n\n" +
                "Open the GitHub dialog and log in with your username and token, " +
                "or create:\n" +
                authStore.tokenPath()
        )
    }

    internal fun publishAccounts() {
        return this.__splitImpl_publishAccounts()
    }

    /**
     * Reads every account in token.env and asks GitHub which username each
     * token belongs to. Accounts with a bad token are skipped; if EVERY token
     * fails, the first error is thrown like before.
     */
    internal fun reloadAccounts(): List<String> {
        return this.__splitImpl_reloadAccounts()
    }

    /**
     * Returns the account currently in use.
     * [reload] re-reads token.env (used by refresh / connect).
     */
    internal suspend fun externalGithubAuth(reload: Boolean = false): RexGitAuth? {
        return this.__splitImpl_externalGithubAuth(reload)
    }

    // ---------- home ----------

    internal fun loadInitial() {
        return this.__splitImpl_loadInitial()
    }

    fun refresh() {
        return this.__splitImpl_refresh()
    }

    // ---------- repository ----------

    fun selectRepository(repository: RexGitRepository) {
        return this.__splitImpl_selectRepository(repository)
    }

    fun backToRepositories() {
        return this.__splitImpl_backToRepositories()
    }

    internal fun reloadRepository(repo: RexGitRepository) {
        return this.__splitImpl_reloadRepository(repo)
    }

    fun refreshStatus() {
        return this.__splitImpl_refreshStatus()
    }

    // ---------- files ----------

    fun openDirectory(directory: String) {
        return this.__splitImpl_openDirectory(directory)
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
        return this.__splitImpl_openParent()
    }

    fun openFile(file: RexGitFile) {
        return this.__splitImpl_openFile(file)
    }

    internal fun looksBinary(file: File): Boolean {
        return this.__splitImpl_looksBinary(file)
    }

    // ---------- create / delete ----------

    fun createFile(name: String) {
        return this.__splitImpl_createFile(name)
    }

    fun createFolder(name: String) {
        return this.__splitImpl_createFolder(name)
    }

    fun deleteEntry(file: RexGitFile) {
        return this.__splitImpl_deleteEntry(file)
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
        return this.__splitImpl_saveEditor()
    }

    internal fun readReadme(repositoryPath: String): String {
        return this.__splitImpl_readReadme(repositoryPath)
    }

    internal fun repositorySlug(repo: RexGitRepository): Pair<String, String>? {
        return this.__splitImpl_repositorySlug(repo)
    }

    internal fun loadRepositoryGithubData(repo: RexGitRepository) {
        return this.__splitImpl_loadRepositoryGithubData(repo)
    }

    fun refreshRepositoryHub() {
        val repo = _state.value.selectedRepository ?: return
        _state.update { it.copy(isRepoHubLoading = true, repoHubError = null, actionsError = null, releasesError = null) }
        loadRepositoryGithubData(repo)
    }

    fun downloadGithubAsset(url: String, rawName: String) {
        return this.__splitImpl_downloadGithubAsset(url, rawName)
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
        return this.__splitImpl_connectGithub(tokenFromUi)
    }

    fun disconnectGithub() {
        return this.__splitImpl_disconnectGithub()
    }

    /**
     * Logs in with a typed username + token, checks the token with GitHub and
     * saves it to token.env (slot 1, or slot 2 when an account already exists).
     */
    fun addAccount(username: String, token: String) {
        return this.__splitImpl_addAccount(username, token)
    }

    /** Switches which connected account is used for the GitHub list, clone, pull and push. */
    fun switchAccount(index: Int) {
        return this.__splitImpl_switchAccount(index)
    }

    /** Removes an account from token.env. */
    fun removeAccount(index: Int) {
        return this.__splitImpl_removeAccount(index)
    }


    internal fun fetchGithubRepositories(
        auth: RexGitAuth
    ) {
        return this.__splitImpl_fetchGithubRepositories(auth)
    }

    fun searchGithubRepositories(query: String) {
        return this.__splitImpl_searchGithubRepositories(query)
    }

    fun reloadGithub() {
        return this.__splitImpl_reloadGithub()
    }

    fun cloneRepository(
        repository: RexGitGithubRepository
    ) {
        return this.__splitImpl_cloneRepository(repository)
    }

    // ---------- pull / push ----------

    fun pull() {
        return this.__splitImpl_pull()
    }

    fun push(
        message: String,
        bumpVersion: Boolean,
        commitType: RexGitCommitType = RexGitCommitType.FIX,
        commitFolder: String = "",
        useGitDev: Boolean = false
    ) {
        return this.__splitImpl_push(message, bumpVersion, commitType, commitFolder, useGitDev)
    }

    // ---------- GitDev ----------

    /**
     * Saves GitDev mode for the selected repository. Turning it ON needs a
     * package path that really contains folders (e.g. com/rexaps).
     */
    fun saveGitDev(config: RexGitGitDevConfig) {
        return this.__splitImpl_saveGitDev(config)
    }


    fun clearPushState() {
        return this.__splitImpl_clearPushState()
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
