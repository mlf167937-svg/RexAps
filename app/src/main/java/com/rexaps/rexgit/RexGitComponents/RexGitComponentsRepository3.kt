@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_RepositoryCard(
    repo: RexGitRepository,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(
                icon = Icons.Default.Folder,
                tint = if (repo.isGitRepository) scheme.primary else scheme.onSurfaceVariant,
                size = 48.dp
            )

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    repo.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (repo.globalClone) {
                        "Global / ${repo.globalOwner ?: "Unknown"}/${repo.name}"
                    } else {
                        "${repo.accountNickname ?: "Local"} / ${repo.name}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))

                InfoPill(
                    icon = if (repo.isGitRepository) Icons.Default.CallSplit else Icons.Default.Folder,
                    text = if (repo.isGitRepository) "Git repository" else "Plain folder",
                    container = scheme.surfaceVariant,
                    content = scheme.onSurfaceVariant,
                    compact = true
                )
            }

            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = scheme.onSurfaceVariant
            )
        }
    }
}

/** Branch / clean / changes pills (same as before). */
@Composable
internal fun __splitImpl_RepositoryStatusPills(
    state: RexGitUiState,
    repo: RexGitRepository,
    changes: List<RexGitChange>
) {
    val scheme = MaterialTheme.colorScheme

    if (repo.isGitRepository) {
        InfoPill(
            icon = Icons.Default.CallSplit,
            text = repo.branch,
            container = scheme.secondaryContainer,
            content = scheme.onSecondaryContainer
        )

        if (changes.isEmpty() && state.gitStatus.error == null) {
            InfoPill(
                icon = Icons.Default.CheckCircle,
                text = "Clean",
                container = scheme.primaryContainer,
                content = scheme.onPrimaryContainer
            )
        } else if (changes.isNotEmpty()) {
            InfoPill(
                icon = Icons.Default.Warning,
                text = if (changes.size == 1) "1 change" else "${changes.size} changes",
                container = scheme.tertiaryContainer,
                content = scheme.onTertiaryContainer
            )
        }
    } else {
        InfoPill(
            icon = Icons.Default.Warning,
            text = "Not a Git repository",
            container = scheme.errorContainer,
            content = scheme.onErrorContainer
        )
    }
}

/** GitDev + account pills. Tapping them opens the matching dialog. */
@Composable
internal fun __splitImpl_RepositoryToolPills(
    state: RexGitUiState,
    repo: RexGitRepository,
    onGitDev: () -> Unit,
    onAccount: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    if (!repo.isGitRepository) return

    val ready = state.gitDev.isReady

    TappablePill(onClick = onGitDev) {
        InfoPill(
            icon = if (ready) Icons.Default.LockOpen else Icons.Default.Lock,
            text = if (ready) "GitDev on" else "GitDev off",
            container = if (ready) scheme.primaryContainer else scheme.surfaceVariant,
            content = if (ready) scheme.onPrimaryContainer else scheme.onSurfaceVariant
        )
    }

    TappablePill(onClick = onAccount) {
        InfoPill(
            icon = Icons.Default.Person,
            text = state.githubUsername?.let { "@$it" } ?: "No account",
            container = scheme.surfaceVariant,
            content = scheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun __splitImpl_RexGitLogo() {
    val scheme = MaterialTheme.colorScheme

    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(scheme.primary, scheme.secondary))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Code,
            contentDescription = null,
            tint = scheme.onPrimary
        )
    }
}
