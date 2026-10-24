package com.rexaps.rexgit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
        internal const val MAX_EDITOR_BYTES = 400_000L

        internal const val ACTIVE_KEY = "active_username"
        internal const val REPO_ACCOUNT_PREFIX = "repo_account:"
    }

    init {
        RexGitStorage.init(application)
        RexGitJGit.init(application)
        loadInitial()
    }

    fun refresh() = refreshImpl()

    fun selectRepository(repository: RexGitRepository) = selectRepositoryImpl(repository)
    fun backToRepositories() = backToRepositoriesImpl()
    fun refreshStatus() = refreshStatusImpl()

    fun openDirectory(directory: String) = openDirectoryImpl(directory)
    fun openRoot() = openRootImpl()
    fun openParent() = openParentImpl()
    fun openFile(file: RexGitFile) = openFileImpl(file)

    fun createFile(name: String) = createFileImpl(name)
    fun createFolder(name: String) = createFolderImpl(name)
    fun deleteEntry(file: RexGitFile) = deleteEntryImpl(file)

    fun editorText(): String = editorTextImpl()
    fun updateEditor(content: String) = updateEditorImpl(content)
    fun closeEditor() = closeEditorImpl()
    fun saveEditor() = saveEditorImpl()
    fun refreshRepositoryHub() = refreshRepositoryHubImpl()
    fun downloadGithubAsset(url: String, rawName: String) = downloadGithubAssetImpl(url, rawName)


    fun connectGithub(
        @Suppress("UNUSED_PARAMETER")
        tokenFromUi: String
    ) = connectGithubImpl(tokenFromUi)

    fun disconnectGithub() = disconnectGithubImpl()
    fun addAccount(username: String, token: String) = addAccountImpl(username, token)
    fun switchAccount(index: Int) = switchAccountImpl(index)
    fun removeAccount(index: Int) = removeAccountImpl(index)
    fun searchGithubRepositories(query: String) = searchGithubRepositoriesImpl(query)
    fun reloadGithub() = reloadGithubImpl()

    fun cloneRepository(
        repository: RexGitGithubRepository
    ) = cloneRepositoryImpl(repository)


    fun pull() = pullImpl()

    fun push(
        message: String,
        bumpVersion: Boolean,
        commitType: RexGitCommitType = RexGitCommitType.FIX,
        commitFolder: String = "",
        useGitDev: Boolean = false
    ) = pushImpl(message, bumpVersion, commitType, commitFolder, useGitDev)


    fun saveGitDev(config: RexGitGitDevConfig) = saveGitDevImpl(config)
    fun clearPushState() = clearPushStateImpl()

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
