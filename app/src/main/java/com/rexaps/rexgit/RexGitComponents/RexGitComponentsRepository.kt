
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.rexaps.rexgit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person

/** Branch / clean / changes pills (same as before). */
@Composable
internal fun RepositoryStatusPills(
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
internal fun RepositoryToolPills(
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

/** Breadcrumb + file list + "New" button + status error. Give it Modifier.weight(1f). */
@Composable
internal fun RepositoryFiles(
    state: RexGitUiState,
    repo: RexGitRepository,
    onOpen: (RexGitFile) -> Unit,
    onMore: (RexGitFile) -> Unit,
    onNavigate: (String?) -> Unit,
    onNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    val changes = state.gitStatus.changes
    val changeMap = remember(changes) { changes.associate { it.path to it.kind } }

    val relative = state.currentDirectory
        ?.removePrefix(repo.path)
        ?.trim('/')
        ?: ""

    val segments = remember(relative) {
        relative.split('/').filter { it.isNotEmpty() }
    }

    Column(modifier) {

        // ----- breadcrumb -----
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val rootActive = segments.isEmpty()

            Crumb(
                text = repo.name,
                active = rootActive,
                onClick = { onNavigate(null) }
            )

            segments.forEachIndexed { index, segment ->
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = scheme.onSurfaceVariant
                )

                val target = repo.path + "/" + segments.take(index + 1).joinToString("/")

                Crumb(
                    text = segment,
                    active = index == segments.lastIndex,
                    onClick = { onNavigate(target) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // ----- files -----
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                color = scheme.surface,
                border = BorderStroke(1.dp, scheme.outlineVariant)
            ) {
                if (state.files.isEmpty() && !state.isRepoLoading) {
                    EmptyState(
                        icon = Icons.Default.FolderOpen,
                        title = "This folder is empty",
                        body = "Create a file or folder to get started.",
                        modifier = Modifier.fillMaxSize(),
                        actionLabel = "Create new",
                        onAction = onNew
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 6.dp, bottom = 96.dp)
                    ) {
                        itemsIndexed(state.files, key = { _, f -> f.path }) { index, file ->
                            val rel = file.path.removePrefix(repo.path).trimStart('/')
                            val kind = if (file.isDirectory) null else changeMap[rel]
                            val dirChanged =
                                file.isDirectory && changeMap.keys.any { it.startsWith("$rel/") }

                            Column {
                                if (index > 0) {
                                    Box(
                                        Modifier
                                            .padding(start = 68.dp)
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(scheme.outlineVariant.copy(alpha = 0.6f))
                                    )
                                }

                                FileRow(
                                    file = file,
                                    change = kind,
                                    hasChangesInside = dirChanged,
                                    onClick = { onOpen(file) },
                                    onMore = { onMore(file) }
                                )
                            }
                        }
                    }
                }
            }

            ExtendedFloatingActionButton(
                onClick = onNew,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New") },
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 28.dp, bottom = 16.dp)
            )
        }

        if (state.gitStatus.error != null) {
            Text(
                "Status unavailable: ${state.gitStatus.error}",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
