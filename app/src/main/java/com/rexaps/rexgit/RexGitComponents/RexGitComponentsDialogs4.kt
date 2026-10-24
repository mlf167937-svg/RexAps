@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

/**
 * GitHub accounts.
 *
 * - No account yet: log in with username + token (or load token.env).
 * - Logged in: pick the account to use, add a second one, or remove one.
 */
@Composable
internal fun __splitImpl_GithubDialog(
    state: RexGitUiState,
    onDismiss: () -> Unit,
    onLoadEnv: () -> Unit,
    onAdd: (username: String, token: String) -> Unit,
    onSwitch: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onDisconnect: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    var adding by remember { mutableStateOf(false) }
    var removeIndex by remember { mutableStateOf<Int?>(null) }

    val wasConnected = remember { state.isGithubConnected }
    val accountCount = state.accounts.size

    LaunchedEffect(state.isGithubConnected) {
        if (state.isGithubConnected && !wasConnected) onDismiss()
    }

    LaunchedEffect(accountCount) {
        adding = false
    }

    RexGitDialogFrame(
        title = "GitHub accounts",
        onDismiss = onDismiss,
        actions = {
            if (state.isGithubConnected) {
                TextButton(onClick = onDisconnect) { Text("Disconnect") }
            }

            TextButton(onClick = onDismiss) { Text("Close") }
        }
    ) {
        if (accountCount > 0) {
            Text(
                "Tap an account to use it for repositories, clone, pull and push.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )

            state.accounts.forEachIndexed { index, name ->
                AccountRow(
                    name = name,
                    active = index == state.activeAccount,
                    enabled = !state.isGithubLoading,
                    onClick = { onSwitch(index) },
                    onRemove = { removeIndex = index }
                )
            }

            if (accountCount < RexGitAuthStore.MAX_ACCOUNTS) {
                if (adding) {
                    Text("Add another account", style = MaterialTheme.typography.titleSmall)

                    AccountForm(
                        loading = state.isGithubLoading,
                        submitLabel = "Add account",
                        onSubmit = onAdd,
                        onCancel = { adding = false }
                    )
                } else {
                    OutlinedButton(
                        onClick = { adding = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Add account")
                    }
                }
            } else {
                Text(
                    "Unable to add another account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                "Log in with your GitHub username and a personal access token.",
                style = MaterialTheme.typography.bodyMedium
            )

            AccountForm(
                loading = state.isGithubLoading,
                submitLabel = "Connect",
                onSubmit = onAdd,
                onCancel = null
            )

            Text(
                "Already have a token.env file in Download/RexAps/Github?",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onLoadEnv,
                enabled = !state.isGithubLoading,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("Load token.env")
            }
        }

        Text(
            "Accounts are saved in Download/RexAps/Github/token.env as " +
                "Account-1-Username, Account-1-Token, Account-2-Username, Account-2-Token, and so on. " +
                "Anyone who can read that file can use the tokens.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
    }

    removeIndex?.let { index ->
        val name = state.accounts.getOrNull(index)

        if (name == null) {
            removeIndex = null
        } else {
            AlertDialog(
                onDismissRequest = { removeIndex = null },
                icon = {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = scheme.error)
                },
                title = { Text("Remove @$name?") },
                text = {
                    Text(
                        "This deletes the account from token.env. " +
                            "You can add it again later with its token."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            removeIndex = null
                            onRemove(index)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.error,
                            contentColor = scheme.onError
                        )
                    ) { Text("Remove") }
                },
                dismissButton = {
                    TextButton(onClick = { removeIndex = null }) { Text("Cancel") }
                }
            )
        }
    }
}
