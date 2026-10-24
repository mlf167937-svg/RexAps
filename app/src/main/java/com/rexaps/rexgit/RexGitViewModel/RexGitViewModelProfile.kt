package com.rexaps.rexgit

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update

/**
     * Logs in with a typed username + token, checks the token with GitHub and
     * saves it to token.env (slot 1, or slot 2 when an account already exists).
     */
    internal fun RexGitViewModel.__splitImpl_addAccount(username: String, token: String) {
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

/** Removes an account from token.env. */
    internal fun RexGitViewModel.__splitImpl_removeAccount(index: Int) {
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

/**
     * Reads every account in token.env and asks GitHub which username each
     * token belongs to. Accounts with a bad token are skipped; if EVERY token
     * fails, the first error is thrown like before.
     */
    internal fun RexGitViewModel.__splitImpl_reloadAccounts(): List<String> {
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

/** Switches which connected account is used for the GitHub list, clone, pull and push. */
    internal fun RexGitViewModel.__splitImpl_switchAccount(index: Int) {
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

internal fun RexGitViewModel.__splitImpl_publishAccounts() {
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
