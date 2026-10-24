package com.rexaps.rexgit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
        internal const val MAX_EDITOR_BYTES = 400_000L

        internal const val ACTIVE_KEY = "active_username"
        internal const val REPO_ACCOUNT_PREFIX = "repo_account:"
    }

    init {
        RexGitStorage.init(application)
        RexGitJGit.init(application)
        loadInitial()
    }

    // ---------- helpers ----------

    /** Runs on IO and converts ANY throwable (including Errors) into a UI error instead of a crash. */
    internal fun launchIO(block: suspend () -> Unit) = this.__split_launchIO_1(block)

    internal fun fail(text: String) = this.__splitExtra_fail_1(text)

    internal fun storageInfo(): Pair<String, Boolean> = this.__splitExtra_storageInfo_2()

    internal fun missingAuthError(): IllegalStateException = this.__split_missingAuthError_2()

    internal fun publishAccounts() = this.__split_publishAccounts_3()

    /**
     * Reads every account in token.env and asks GitHub which username each
     * token belongs to. Accounts with a bad token are skipped; if EVERY token
     * fails, the first error is thrown like before.
     */
    internal fun reloadAccounts(): List<String> = this.__split_reloadAccounts_4()

    /**
     * Returns the account currently in use.
     * [reload] re-reads token.env (used by refresh / connect).
     */
    internal suspend fun externalGithubAuth(reload: Boolean = false): RexGitAuth? = this.__split_externalGithubAuth_5(reload)

    // ---------- home ----------

    private fun loadInitial() = this.__split_loadInitial_6()

    fun refresh() = this.__split_refresh_7()

    // ---------- repository ----------

    fun selectRepository(repository: RexGitRepository) = this.__split_selectRepository_8(repository)

    fun backToRepositories() = this.__split_backToRepositories_9()

    internal fun reloadRepository(repo: RexGitRepository) = this.__split_reloadRepository_10(repo)

    fun refreshStatus() = this.__split_refreshStatus_11()

    // ---------- files ----------

    fun openDirectory(directory: String) = this.__split_openDirectory_12(directory)

    fun openRoot() = this.__split_openRoot_13()

    fun openParent() = this.__split_openParent_14()

    fun openFile(file: RexGitFile) = this.__split_openFile_15(file)

    internal fun looksBinary(file: File): Boolean = this.__split_looksBinary_16(file)

    // ---------- create / delete ----------

    fun createFile(name: String) = this.__split_createFile_17(name)

    fun createFolder(name: String) = this.__split_createFolder_18(name)

    fun deleteEntry(file: RexGitFile) = this.__split_deleteEntry_19(file)

    // ---------- editor ----------

    /**
     * Latest editor text. The UI reads it when the layout switches between
     * portrait and landscape so unsaved typing is never lost.
     */
    fun editorText(): String = this.__splitExtra_editorText_3()

    fun updateEditor(content: String) = this.__split_updateEditor_20(content)

    fun closeEditor() = this.__split_closeEditor_21()

    fun saveEditor() = this.__split_saveEditor_22()

    internal fun readReadme(repositoryPath: String): String = this.__split_readReadme_23(repositoryPath)

    internal fun repositorySlug(repo: RexGitRepository): Pair<String, String>? = this.__split_repositorySlug_24(repo)

    internal fun loadRepositoryGithubData(repo: RexGitRepository) = this.__split_loadRepositoryGithubData_25(repo)

    fun refreshRepositoryHub() = this.__split_refreshRepositoryHub_26()

    fun downloadGithubAsset(url: String, rawName: String) = this.__split_downloadGithubAsset_27(url, rawName)

    // ---------- GitHub ----------

    /**
     * Loads the account(s) from token.env. The parameter is ignored; it stays
     * so older callers keep compiling. To log in with a typed username and
     * token use [addAccount].
     */
    fun connectGithub(
        @Suppress("UNUSED_PARAMETER")
        tokenFromUi: String
    ) = this.__split_connectGithub_28(tokenFromUi)

    fun disconnectGithub() = this.__split_disconnectGithub_29()

    /**
     * Logs in with a typed username + token, checks the token with GitHub and
     * saves it to token.env (slot 1, or slot 2 when an account already exists).
     */
    fun addAccount(username: String, token: String) = this.__split_addAccount_30(username, token)

    /** Switches which connected account is used for the GitHub list, clone, pull and push. */
    fun switchAccount(index: Int) = this.__split_switchAccount_31(index)

    /** Removes an account from token.env. */
    fun removeAccount(index: Int) = this.__split_removeAccount_32(index)


    internal fun fetchGithubRepositories(
        auth: RexGitAuth
    ) = this.__split_fetchGithubRepositories_33(auth)

    fun searchGithubRepositories(query: String) = this.__split_searchGithubRepositories_34(query)

    fun reloadGithub() = this.__split_reloadGithub_35()

    fun cloneRepository(
        repository: RexGitGithubRepository
    ) = this.__split_cloneRepository_36(repository)

    // ---------- pull / push ----------

    fun pull() = this.__split_pull_37()

    fun push(
        message: String,
        bumpVersion: Boolean,
        commitType: RexGitCommitType = RexGitCommitType.FIX,
        commitFolder: String = "",
        useGitDev: Boolean = false
    ) = this.__split_push_38(message, bumpVersion, commitType, commitFolder, useGitDev)

    // ---------- GitDev ----------

    /**
     * Saves GitDev mode for the selected repository. Turning it ON needs a
     * package path that really contains folders (e.g. com/rexaps).
     */
    fun saveGitDev(config: RexGitGitDevConfig) = this.__split_saveGitDev_39(config)


    fun clearPushState() = this.__split_clearPushState_40()

    // ---------- messages ----------

    fun dismissMessage() = this.__splitExtra_dismissMessage_4()

    fun dismissError() = this.__splitExtra_dismissError_5()

    fun clearMessage() = this.__splitExtra_clearMessage_6()
}
