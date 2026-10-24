package com.rexaps.rexgit

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update

fun RexGitViewModel.__split_disconnectGithub_29() {
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
    fun RexGitViewModel.__split_addAccount_30(username: String, token: String) {
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
    fun RexGitViewModel.__split_switchAccount_31(index: Int) {
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
    fun RexGitViewModel.__split_removeAccount_32(index: Int) {
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

internal fun RexGitViewModel.__split_fetchGithubRepositories_33(
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

fun RexGitViewModel.__split_searchGithubRepositories_34(query: String) {
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

fun RexGitViewModel.__split_reloadGithub_35() {
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
