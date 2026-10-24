package com.rexaps.rexgit

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update

/** Removes an account from token.env. */
internal fun RexGitViewModel.removeAccountImpl(index: Int) {
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
