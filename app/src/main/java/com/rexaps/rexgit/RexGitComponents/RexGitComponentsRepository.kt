@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

/** Breadcrumb + file list + "New" button + status error. Give it Modifier.weight(1f). */
@Composable
internal fun __splitImpl_RepositoryFiles(
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
