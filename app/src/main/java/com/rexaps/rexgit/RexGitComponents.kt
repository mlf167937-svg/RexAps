
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.res.Configuration
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import java.io.File


// ============================================================
// Repository pieces shared by portrait and landscape
// ============================================================

@Composable
internal fun HomeHeader(
    state: RexGitUiState,
    onRefresh: () -> Unit,
    onGithub: () -> Unit,
    compact: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                start = 20.dp,
                end = 16.dp,
                top = if (compact) 8.dp else 12.dp,
                bottom = if (compact) 8.dp else 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RexGitLogo()

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text("RexGit", style = MaterialTheme.typography.headlineSmall)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.isGithubConnected) scheme.secondary else scheme.outline
                        )
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    if (state.isGithubConnected) "@${state.githubUsername}" + accountSuffix(state)
                    else "GitHub not connected",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        HeaderIconButton(
            icon = Icons.Default.Refresh,
            description = "Refresh repositories",
            onClick = onRefresh
        )

        Spacer(Modifier.width(8.dp))

        HeaderIconButton(
            icon = Icons.Default.Cloud,
            description =
                if (state.isGithubConnected) "GitHub account, connected"
                else "Connect GitHub",
            onClick = onGithub,
            tint = if (state.isGithubConnected) scheme.primary else scheme.onSurfaceVariant
        )
    }
}

internal fun accountSuffix(state: RexGitUiState): String {
    return if (state.accounts.size > 1) "  ·  ${state.accounts.size} accounts" else ""
}

/** Location text used by the "new file / folder" sheets and dialogs. */
internal fun locationLabel(state: RexGitUiState): String {
    val repo = state.selectedRepository ?: return ""

    val relative = state.currentDirectory
        ?.removePrefix(repo.path)
        ?.trim('/')
        ?: ""

    val segments = relative.split('/').filter { it.isNotEmpty() }

    return if (segments.isEmpty()) repo.name
    else repo.name + "/" + segments.joinToString("/")
}

@Composable
internal fun RepositoryHeader(
    repo: RexGitRepository,
    busy: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    compact: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = if (compact) 8.dp else 12.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderIconButton(
            icon = Icons.Default.ArrowBack,
            description = "Back to repositories",
            onClick = onBack
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                repo.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!repo.remoteUrl.isNullOrBlank()) {
                Text(
                    repo.remoteUrl.removePrefix("https://"),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        HeaderIconButton(
            icon = Icons.Default.Refresh,
            description = "Refresh status",
            onClick = onRefresh,
            enabled = !busy
        )
    }
}

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

@Composable
internal fun TappablePill(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        content()
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

/**
 * Pull / Push bar. Push shows "Unlock Push" until GitDev mode is on
 * and a package path is filled in.
 */
@Composable
internal fun ActionBar(
    isGit: Boolean,
    busy: Boolean,
    pulling: Boolean,
    pushing: Boolean,
    changeCount: Int,
    gitDevReady: Boolean,
    onPull: () -> Unit,
    onPush: () -> Unit,
    onUnlockPush: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Surface(color = scheme.surface) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(scheme.outlineVariant)
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onPull,
                    enabled = isGit && !busy,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, scheme.outline),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                ) {
                    if (pulling) ButtonSpinner()
                    else Icon(Icons.Default.CloudDownload, contentDescription = null)

                    Spacer(Modifier.width(8.dp))
                    Text(if (pulling) "Pulling" else "Pull")
                }

                if (gitDevReady) {
                    GradientButton(
                        text = when {
                            pushing -> "Pushing"
                            changeCount > 0 -> "Push ($changeCount)"
                            else -> "Push"
                        },
                        icon = Icons.Default.CloudUpload,
                        onClick = onPush,
                        enabled = isGit && !busy,
                        loading = pushing,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    OutlinedButton(
                        onClick = onUnlockPush,
                        enabled = isGit && !busy,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, scheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Unlock Push", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
internal fun Crumb(
    text: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = if (active) scheme.primary else scheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !active, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    )
}

@Composable
internal fun ChangesPanel(
    changes: List<RexGitChange>,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        color = scheme.surface,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = if (expanded) "Hide changes" else "Show changes",
                        onClick = onToggle
                    )
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Changes (${changes.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    if (expanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant
                )
            }

            if (expanded) {
                LazyColumn(
                    Modifier.heightIn(max = 200.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(changes.take(300), key = { it.kind.name + it.path }) { change ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ChangeBadge(change.kind)

                            Spacer(Modifier.width(10.dp))

                            Text(
                                change.path,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (changes.size > 300) {
                        item(key = "more") {
                            Text(
                                "+ ${changes.size - 300} more",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ============================================================
// Create / delete UI
// ============================================================

@Composable
internal fun CreateSheet(
    location: String,
    onDismiss: () -> Unit,
    onPick: (CreateKind) -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surface
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Text("Create new", style = MaterialTheme.typography.titleLarge)

            Text(
                "in $location",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            SheetAction(
                icon = Icons.Default.NoteAdd,
                title = "New file",
                subtitle = "Create an empty file and open it in the editor",
                onClick = { onPick(CreateKind.FILE) }
            )

            SheetAction(
                icon = Icons.Default.CreateNewFolder,
                title = "New folder",
                subtitle = "Create an empty folder here",
                onClick = { onPick(CreateKind.FOLDER) }
            )
        }
    }
}

@Composable
internal fun ItemActionsSheet(
    file: RexGitFile,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val style = fileStyle(file, scheme)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surface
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconTile(style.icon, style.color)

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        file.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        if (file.isDirectory) "Folder" else formatSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            SheetAction(
                icon = if (file.isDirectory) Icons.Default.FolderOpen else Icons.Default.Description,
                title = if (file.isDirectory) "Open folder" else "Edit file",
                subtitle = null,
                onClick = onOpen
            )

            SheetAction(
                icon = Icons.Default.Delete,
                title = if (file.isDirectory) "Delete folder" else "Delete file",
                subtitle = if (file.isDirectory) "Removes the folder and everything inside it"
                else "Permanently removes this file",
                destructive = true,
                onClick = onDelete
            )
        }
    }
}

@Composable
internal fun SheetAction(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val tint = if (destructive) scheme.error else scheme.primary

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon, tint)

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (destructive) scheme.error else scheme.onSurface
            )

            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun NameDialog(
    kind: CreateKind,
    location: String,
    existing: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    val error: String? = when {
        name.isBlank() -> null
        validateEntryName(name) != null -> validateEntryName(name)
        existing.any { it.equals(name.trim(), ignoreCase = true) } ->
            "\"${name.trim()}\" already exists in this folder"
        else -> null
    }

    val canCreate = name.isNotBlank() && error == null
    val isFile = kind == CreateKind.FILE

    LaunchedEffect(Unit) {
        runCatching { focus.requestFocus() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isFile) "New file" else "New folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "in $location",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus),
                    label = { Text(if (isFile) "File name" else "Folder name") },
                    placeholder = { Text(if (isFile) "MainActivity.kt" else "components") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = {
                        Text(error ?: if (isFile) "Include the extension, e.g. notes.md" else "Letters, numbers, dots and dashes")
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (canCreate) onConfirm(name) }
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = canCreate
            ) {
                Text(if (isFile) "Create file" else "Create folder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
internal fun DeleteDialog(
    file: RexGitFile,
    isGit: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Delete, contentDescription = null, tint = scheme.error)
        },
        title = { Text(if (file.isDirectory) "Delete folder?" else "Delete file?") },
        text = {
            Text(
                if (file.isDirectory)
                    "\"${file.name}\" and everything inside it will be permanently deleted from this device." +
                        (if (isGit) " Uncommitted work inside it can't be recovered." else "")
                else
                    "\"${file.name}\" will be permanently deleted from this device." +
                        (if (isGit) " If it was never committed, it can't be recovered." else "")
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.error,
                    contentColor = scheme.onError
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ============================================================
// Cards & rows
// ============================================================

@Composable
internal fun WorkspaceCard(
    total: Int,
    gitCount: Int,
    path: String
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(scheme.primaryContainer, scheme.secondaryContainer)
                )
            )
            .border(1.dp, scheme.primary.copy(alpha = 0.25f), shape)
            .padding(20.dp)
    ) {
        Text(
            "Workspace",
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onPrimaryContainer.copy(alpha = 0.7f)
        )

        Text(
            "$total",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = scheme.onPrimaryContainer
        )

        Text(
            if (total == 1) "repository" else "repositories",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onPrimaryContainer.copy(alpha = 0.8f)
        )

        Spacer(Modifier.height(14.dp))

        InfoPill(
            icon = Icons.Default.CallSplit,
            text = "$gitCount Git",
            container = scheme.surface.copy(alpha = 0.55f),
            content = scheme.onSurface
        )

        if (path.isNotBlank()) {
            Spacer(Modifier.height(12.dp))

            Text(
                path,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = scheme.onPrimaryContainer.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun RepositoryCard(
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

                Spacer(Modifier.height(4.dp))

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

@Composable
internal fun GithubRepositoryCard(
    repo: RexGitGithubRepository,
    cloned: Boolean,
    cloning: Boolean,
    anyCloning: Boolean,
    onClone: () -> Unit,
    onOpen: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Default.Code, scheme.secondary)

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        repo.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        repo.fullName,
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                InfoPill(
                    icon = if (repo.private) Icons.Default.Lock else Icons.Default.Public,
                    text = if (repo.private) "Private" else "Public",
                    container = scheme.surfaceVariant,
                    content = scheme.onSurfaceVariant,
                    compact = true
                )
            }

            if (!repo.description.isNullOrBlank()) {
                Text(
                    repo.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            if (cloned) {
                OutlinedButton(
                    onClick = onOpen,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Open")
                }
            } else {
                FilledTonalButton(
                    onClick = onClone,
                    enabled = !anyCloning,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    if (cloning) ButtonSpinner()
                    else Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(8.dp))
                    Text(if (cloning) "Cloning" else "Clone")
                }
            }
        }
    }
}

internal data class FileStyle(val icon: ImageVector, val color: Color)

internal fun fileStyle(file: RexGitFile, scheme: ColorScheme): FileStyle {
    if (file.isDirectory) return FileStyle(Icons.Default.Folder, scheme.primary)

    return when (file.name.substringAfterLast('.', "").lowercase()) {
        "kt", "kts", "java", "gradle" -> FileStyle(Icons.Default.Code, scheme.primary)
        "xml", "html", "css", "js", "ts", "json", "yml", "yaml", "toml" ->
            FileStyle(Icons.Default.Code, scheme.secondary)
        "md", "txt" -> FileStyle(Icons.Default.Description, scheme.tertiary)
        "png", "jpg", "jpeg", "webp", "gif", "svg" ->
            FileStyle(Icons.Default.Image, Color(0xFFFF8FB1))
        "properties", "pro", "gitignore" ->
            FileStyle(Icons.Default.Settings, scheme.onSurfaceVariant)
        else -> FileStyle(Icons.Default.InsertDriveFile, scheme.onSurfaceVariant)
    }
}

@Composable
internal fun FileRow(
    file: RexGitFile,
    change: RexGitChangeKind?,
    hasChangesInside: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val style = fileStyle(file, scheme)

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = if (file.isDirectory) "Open folder" else "Open file",
                onClick = onClick
            )
            .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(style.icon, style.color)

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                file.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                if (file.isDirectory) "Folder" else formatSize(file.size),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }

        if (change != null) {
            Spacer(Modifier.width(8.dp))
            ChangeBadge(change)
        } else if (hasChangesInside) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(scheme.tertiary)
                    .semantics { contentDescription = "Contains changes" }
            )
        }

        IconButton(onClick = onMore) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Actions for ${file.name}",
                tint = scheme.onSurfaceVariant
            )
        }
    }
}


// ============================================================
// Dialog frame (scrolls, so it also works in landscape)
// ============================================================

@Composable
internal fun RexGitDialogFrame(
    title: String,
    onDismiss: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.92f).dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .heightIn(max = maxHeight),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.padding(top = 22.dp, bottom = 12.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
    }
}

// ============================================================
// GitHub accounts
// ============================================================

/** Small chips to switch between the connected accounts. */
@Composable
internal fun AccountSwitcher(
    accounts: List<String>,
    active: Int,
    onSwitch: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        accounts.forEachIndexed { index, name ->
            val selected = index == active

            Surface(
                onClick = { onSwitch(index) },
                shape = RoundedCornerShape(12.dp),
                color = if (selected) scheme.primaryContainer else scheme.surfaceVariant,
                contentColor = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                border = BorderStroke(
                    1.dp,
                    if (selected) scheme.primary.copy(alpha = 0.4f) else scheme.outlineVariant
                )
            ) {
                Row(
                    Modifier
                        .heightIn(min = 40.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (selected) Icons.Default.CheckCircle else Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        "@$name",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    name: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = if (active) scheme.primaryContainer.copy(alpha = 0.55f) else scheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (active) scheme.primary.copy(alpha = 0.45f) else scheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .heightIn(min = 64.dp)
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(scheme.primary, scheme.secondary))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onPrimary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "@$name",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    if (active) "In use" else "Tap to use this account",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (active) scheme.primary else scheme.onSurfaceVariant
                )
            }

            if (active) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "In use",
                    tint = scheme.primary
                )
            }

            IconButton(onClick = onRemove, enabled = enabled) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove @$name",
                    tint = scheme.error.copy(alpha = if (enabled) 1f else 0.38f)
                )
            }
        }
    }
}

@Composable
private fun AccountForm(
    loading: Boolean,
    submitLabel: String,
    onSubmit: (username: String, token: String) -> Unit,
    onCancel: (() -> Unit)?
) {
    var username by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("GitHub username") },
            singleLine = true,
            enabled = !loading,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                autoCorrect = false,
                imeAction = ImeAction.Next
            )
        )

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Personal access token") },
            singleLine = true,
            enabled = !loading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrect = false,
                imeAction = ImeAction.Done
            ),
            visualTransformation =
                if (visible) VisualTransformation.None
                else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Default.VisibilityOff
                        else Icons.Default.Visibility,
                        contentDescription = if (visible) "Hide token" else "Show token"
                    )
                }
            }
        )

        Text(
            "Create a token at github.com/settings/tokens with the \"repo\" scope. " +
                "Never share it in chat or commit it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onCancel != null) {
                TextButton(onClick = onCancel, enabled = !loading) { Text("Cancel") }
            }

            Button(
                onClick = { onSubmit(username, token) },
                enabled = !loading && username.isNotBlank() && token.isNotBlank()
            ) {
                if (loading) {
                    ButtonSpinner()
                    Spacer(Modifier.width(8.dp))
                }

                Text(if (loading) "Connecting" else submitLabel)
            }
        }
    }
}

/**
 * GitHub accounts.
 *
 * - No account yet: log in with username + token (or load token.env).
 * - Logged in: pick the account to use, add a second one, or remove one.
 */
@Composable
internal fun GithubDialog(
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
                    "Maximum of ${RexGitAuthStore.MAX_ACCOUNTS} accounts connected.",
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
                "username/token and username2/token2. Anyone who can read that " +
                "file can use the tokens.",
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

// ============================================================
// GitDev mode: guide + setup
// ============================================================

@Composable
private fun GuideHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun GuideText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun GuideCode(text: String) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = scheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = scheme.onSurfaceVariant,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        )
    }
}

@Composable
private fun GitDevGuide() {
    GuideHeading("What GitDev mode does")
    GuideText(
        "Push writes commit messages like  [26.57] Fix: login: your description.  " +
            "The word after the type is a folder of your app. GitDev tells RexGit " +
            "where your app's code lives, so it works for any APK project, not " +
            "only com/rexaps."
    )
    GuideText(
        "Push stays locked until GitDev mode is on and a package path is filled in. " +
            "Settings are saved per repository."
    )

    GuideHeading("How folders are detected")
    GuideText(
        "RexGit opens  <module>/src/main/java/<package path>  and " +
            " <module>/src/main/kotlin/<package path>.  Every folder directly " +
            "inside becomes a choice in the Push dialog. Hidden folders (starting " +
            "with a dot) are ignored, and so is MainActivity.kt. A folder that " +
            "appears in your changed files is added too."
    )

    GuideHeading("Option 1: Package path (required)")
    GuideText(
        "The folder that CONTAINS your feature folders, written as a path " +
            "starting from the first folder after java or kotlin."
    )
    GuideText(
        "Tip: open any .kt file. If it starts with  package com.rexaps.login  " +
            "then login is the feature folder and your package path is  com/rexaps. " +
            "Dots work too: com.rexaps"
    )

    GuideHeading("Option 2: Module folder (optional)")
    GuideText(
        "The Gradle module that holds the code. Leave it empty and RexGit tries " +
            "app/ first, then the project root. Fill it in only for another " +
            "module, for example  feature-login."
    )

    GuideHeading("Example")
    GuideCode(
        "app/src/main/java/com/rexaps/\n" +
            "  login/          -> folder choice: login\n" +
            "  settings/       -> folder choice: settings\n" +
            "  MainActivity.kt -> ignored\n\n" +
            "Package path: com/rexaps"
    )

    GuideHeading("Other apps")
    GuideText("Not every APK uses com/rexaps. Other examples:")
    GuideCode(
        "id/mycompany/notes\n" +
            "org/example/tools\n" +
            "io/github/yourname/app"
    )

    GuideHeading("Before you activate")
    GuideCode(
        "1. Open your repository in RexGit.\n" +
            "2. Browse app > src > main > java (or kotlin).\n" +
            "3. Follow the folders (com > yourname ...) until you reach the\n" +
            "   folder that holds your feature folders.\n" +
            "4. Type that path as the package path.\n" +
            "5. RexGit lists the folders it found. An empty list means the\n" +
            "   path (or module) is wrong."
    )
}

/**
 * GitDev mode. Page 0 is the reading guide (shown before activation),
 * page 1 is the setup form with a live preview of the detected folders.
 */
@Composable
internal fun GitDevDialog(
    repo: RexGitRepository,
    config: RexGitGitDevConfig,
    onSave: (RexGitGitDevConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    var page by rememberSaveable { mutableStateOf(if (config.enabled) 1 else 0) }
    var understood by rememberSaveable { mutableStateOf(false) }
    var packagePath by rememberSaveable { mutableStateOf(config.packagePath) }
    var module by rememberSaveable { mutableStateOf(config.module) }

    var found by remember { mutableStateOf<List<String>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }

    val draft = RexGitGitDev.normalize(
        RexGitGitDevConfig(enabled = true, packagePath = packagePath, module = module)
    )

    LaunchedEffect(draft.packagePath, draft.module) {
        if (draft.packagePath.isBlank()) {
            found = emptyList()
            scanning = false
        } else {
            scanning = true
            delay(250)
            found = withContext(Dispatchers.IO) {
                RexGitGitDev.detectFolders(File(repo.path), draft)
            }
            scanning = false
        }
    }

    if (page == 0) {
        RexGitDialogFrame(
            title = "GitDev mode guide",
            onDismiss = onDismiss,
            actions = {
                TextButton(onClick = onDismiss) { Text("Cancel") }

                Button(
                    onClick = { page = 1 },
                    enabled = understood
                ) { Text("Continue") }
            }
        ) {
            Text(
                "Read this first. It explains how RexGit finds the folders used by Push.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )

            GitDevGuide()

            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = understood,
                        role = Role.Checkbox,
                        onValueChange = { understood = it }
                    )
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = understood, onCheckedChange = null)
                Spacer(Modifier.width(10.dp))
                Text("I have read the guide", style = MaterialTheme.typography.bodyLarge)
            }
        }
    } else {
        val canActivate = draft.packagePath.isNotBlank() && found.isNotEmpty() && !scanning

        RexGitDialogFrame(
            title = "GitDev setup",
            onDismiss = onDismiss,
            actions = {
                if (config.enabled) {
                    TextButton(
                        onClick = {
                            onSave(config.copy(enabled = false))
                            onDismiss()
                        }
                    ) { Text("Turn off") }
                }

                TextButton(onClick = onDismiss) { Text("Cancel") }

                Button(
                    onClick = {
                        onSave(draft)
                        onDismiss()
                    },
                    enabled = canActivate
                ) { Text(if (config.enabled) "Save" else "Activate") }
            }
        ) {
            Text(
                "Repository: ${repo.name}",
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = packagePath,
                onValueChange = { packagePath = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Package path") },
                placeholder = { Text("com/rexaps") },
                singleLine = true,
                supportingText = { Text("Folder that contains your feature folders. Dots work too.") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    autoCorrect = false,
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = module,
                onValueChange = { module = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Module folder (optional)") },
                placeholder = { Text("app") },
                singleLine = true,
                supportingText = { Text("Empty = try app/ first, then the project root.") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    autoCorrect = false,
                    imeAction = ImeAction.Done
                )
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color =
                    if (draft.packagePath.isBlank() || scanning) scheme.surfaceVariant
                    else if (found.isEmpty()) scheme.errorContainer
                    else scheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when {
                        draft.packagePath.isBlank() -> Text(
                            "Fill in the package path to scan for folders.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        scanning -> Text(
                            "Scanning...",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        found.isEmpty() -> {
                            Text(
                                "No folders found. Check the package path or module.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onErrorContainer
                            )

                            Text(
                                "Looked in:\n" + RexGitGitDev.sourceRoots(draft).joinToString("\n"),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = scheme.onErrorContainer
                            )
                        }

                        else -> {
                            Text(
                                if (found.size == 1) "1 folder found"
                                else "${found.size} folders found",
                                style = MaterialTheme.typography.titleSmall,
                                color = scheme.onPrimaryContainer
                            )

                            Text(
                                found.joinToString("  ·  "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onPrimaryContainer,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            TextButton(onClick = { page = 0 }) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Read the guide again")
            }
        }
    }
}


@Composable
internal fun PushDialog(
    message: String,
    onMessageChange: (String) -> Unit,
    type: RexGitCommitType,
    onTypeChange: (RexGitCommitType) -> Unit,
    folder: String,
    folders: List<String>,
    packagePath: String,
    onFolderChange: (String) -> Unit,
    bumpVersion: Boolean,
    onBumpChange: (Boolean) -> Unit,
    changeCount: Int,
    onDismiss: () -> Unit,
    onPush: () -> Unit
) {
    var typeMenuOpen by remember { mutableStateOf(false) }
    var folderMenuOpen by remember { mutableStateOf(false) }

    val canCommit = changeCount == 0 || (
        message.isNotBlank() &&
            folder.isNotBlank() &&
            folders.contains(folder)
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Commit & push") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (changeCount == 0) {
                        "No local changes. Existing commits will be pushed."
                    } else {
                        "$changeCount changed file(s) will be committed and pushed to origin."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                if (changeCount > 0) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.weight(0.9f)) {
                            OutlinedButton(
                                onClick = { typeMenuOpen = true },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text(type.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            DropdownMenu(
                                expanded = typeMenuOpen,
                                onDismissRequest = { typeMenuOpen = false }
                            ) {
                                RexGitCommitType.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            onTypeChange(option)
                                            typeMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        Box(Modifier.weight(1.1f)) {
                            OutlinedButton(
                                onClick = { folderMenuOpen = true },
                                enabled = folders.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text(
                                    folder.ifBlank { "Folder" },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            DropdownMenu(
                                expanded = folderMenuOpen,
                                onDismissRequest = { folderMenuOpen = false }
                            ) {
                                folders.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            onFolderChange(option)
                                            folderMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (folders.isEmpty()) {
                        Text(
                            "No folder found under $packagePath.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    OutlinedTextField(
                        value = message,
                        onValueChange = onMessageChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Description") },
                        placeholder = { Text("RexGit Screen Error") },
                        maxLines = 3,
                        singleLine = false
                    )

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = bumpVersion,
                                role = Role.Switch,
                                onValueChange = onBumpChange
                            )
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Bump version", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Commit prefix uses the new version, e.g. [26.57] Fix: rexgit: ...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.width(12.dp))
                        Switch(checked = bumpVersion, onCheckedChange = null)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onPush,
                enabled = canCommit
            ) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Push")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
internal fun RexPushProgressDialog(
    state: RexGitUiState,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            if (!state.isPushing) onDismiss()
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = 10.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.pushSuccess) {
                    RexPushSuccessAnimation()
                } else {
                    RexPushLoader(active = state.isPushing)
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    if (state.pushSuccess) "✓ RexPush" else "RexPush",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (state.pushSuccess) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    state.pushDetail.ifBlank { state.pushStage },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(18.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            state.pushStage,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.pushSuccess) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )

                        if (state.pushLog.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 170.dp)
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    reverseLayout = true,
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    items(state.pushLog.asReversed()) { line ->
                                        Text(
                                            line,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        if (!state.pushSuccess && state.pushError != null) {
                            Text(
                                state.pushError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (state.pushSuccess) {
                            state.pushCommitMessage?.let { commit ->
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    commit,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (state.pushSuccess) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Successfully pushed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (!state.isPushing) {
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.pushSuccess) "Done" else "Close")
                    }
                }
            }
        }
    }
}

@Composable
internal fun RexPushLoader(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "rexPushLoader")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val scheme = MaterialTheme.colorScheme

    Canvas(Modifier.size(104.dp)) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.34f

        drawCircle(
            color = scheme.outlineVariant.copy(alpha = 0.45f),
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.dp.toPx()
            )
        )

        repeat(8) { index ->
            val angle = Math.toRadians(
                (rotation + index * 45f).toDouble()
            )
            val x = center.x + cos(angle).toFloat() * radius
            val y = center.y + sin(angle).toFloat() * radius
            val emphasis = if (index == 0) 1f else 0.28f
            drawCircle(
                color = scheme.primary.copy(
                    alpha = if (active) emphasis * pulse else 0.25f
                ),
                radius = if (index == 0) 5.dp.toPx() else 3.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
    }
}

@Composable
internal fun RexPushSuccessAnimation() {
    val transition = rememberInfiniteTransition(label = "rexPushSuccess")
    val ring by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "successRing"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "successAlpha"
    )
    val iconScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(420),
        label = "successIcon"
    )

    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier.size(112.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = scheme.primary.copy(alpha = alpha),
                radius = size.minDimension * 0.38f * ring,
                center = androidx.compose.ui.geometry.Offset(
                    size.width / 2f,
                    size.height / 2f
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 3.dp.toPx()
                )
            )

            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension * 0.43f

            listOf(0f, 60f, 120f, 180f, 240f, 300f).forEach { deg ->
                val a = Math.toRadians(deg.toDouble())
                drawCircle(
                    color = scheme.primary.copy(alpha = 0.28f),
                    radius = 2.5.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(
                        cx + cos(a).toFloat() * r,
                        cy + sin(a).toFloat() * r
                    )
                )
            }
        }

        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = scheme.primary,
            modifier = Modifier
                .size(62.dp)
                .scale(iconScale)
        )
    }
}

@Composable
internal fun ErrorDialog(
    text: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Something went wrong") },
        text = {
            Box(
                Modifier
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

// ============================================================
// Shared building blocks
// ============================================================

@Composable
internal fun RexGitLogo() {
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

@Composable
internal fun IconTile(
    icon: ImageVector,
    tint: Color,
    size: Dp = 40.dp
) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
internal fun HeaderIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = description,
                tint = if (enabled) tint else tint.copy(alpha = 0.38f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
internal fun SegmentedTabs(
    labels: List<String>,
    icons: List<ImageVector>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected

            val bg by animateColorAsState(
                if (isSelected) scheme.surface else Color.Transparent
            )
            val fg by animateColorAsState(
                if (isSelected) scheme.primary else scheme.onSurfaceVariant
            )

            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        onClick = { onSelect(index) }
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icons[index],
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(8.dp))

                Text(label, color = fg, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Reserves space for the progress bar so the layout never jumps. */
@Composable
internal fun ProgressSlot(visible: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(3.dp)
    ) {
        if (visible) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun ButtonSpinner() {
    CircularProgressIndicator(
        modifier = Modifier.size(18.dp),
        strokeWidth = 2.dp
    )
}

@Composable
internal fun GradientButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme

    val brush: Brush =
        if (enabled || loading) Brush.linearGradient(listOf(scheme.primary, scheme.secondary))
        else SolidColor(scheme.onSurface.copy(alpha = 0.12f))

    val content =
        if (enabled || loading) scheme.onPrimary
        else scheme.onSurface.copy(alpha = 0.38f)

    Row(
        modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = content
            )
        } else {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(8.dp))

        Text(text, color = content, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, shape)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.width(10.dp))

        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }) {
                Icon(Icons.Default.Close, contentDescription = "Clear search")
            }
        } else {
            Spacer(Modifier.width(10.dp))
        }
    }
}

@Composable
internal fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(scheme.primaryContainer, scheme.secondaryContainer)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = scheme.onPrimaryContainer
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))

            GradientButton(
                text = actionLabel,
                icon = Icons.Default.Add,
                onClick = onAction
            )
        }
    }
}

@Composable
internal fun StorageNotice(
    path: String,
    onGrant: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = scheme.onTertiaryContainer
            )

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    "Using private app storage",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onTertiaryContainer
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    "Downloads isn't writable, so repositories are stored in:\n$path\n\n" +
                        "Allow \"All files access\" to use /Download/RexAps instead.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onTertiaryContainer.copy(alpha = 0.85f)
                )

                TextButton(onClick = onGrant) { Text("Grant access") }
            }
        }
    }
}

@Composable
internal fun InfoPill(
    icon: ImageVector,
    text: String,
    container: Color,
    content: Color,
    compact: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = container,
        contentColor = content
    ) {
        Row(
            Modifier.padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 4.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(if (compact) 14.dp else 16.dp))

            Spacer(Modifier.width(6.dp))

            Text(
                text,
                style =
                    if (compact) MaterialTheme.typography.labelMedium
                    else MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun ChangeBadge(kind: RexGitChangeKind) {
    val scheme = MaterialTheme.colorScheme

    val (bg, fg) = when (kind) {
        RexGitChangeKind.ADDED -> scheme.primaryContainer to scheme.onPrimaryContainer
        RexGitChangeKind.MODIFIED -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        RexGitChangeKind.DELETED -> scheme.errorContainer to scheme.onErrorContainer
        RexGitChangeKind.CONFLICT -> scheme.errorContainer to scheme.onErrorContainer
        RexGitChangeKind.UNTRACKED -> scheme.secondaryContainer to scheme.onSecondaryContainer
    }

    Box(
        Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .semantics { contentDescription = kind.label },
        contentAlignment = Alignment.Center
    ) {
        Text(
            kind.symbol,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

// ============================================================
// Utilities
// ============================================================

internal fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "%.1f MB".format(bytes / 1048576.0)
    }
}

internal fun openAllFilesAccess(context: Context) {
    val uri = Uri.parse("package:${context.packageName}")

    try {
        val intent =
            if (Build.VERSION.SDK_INT >= 30) {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
            }

        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Throwable) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Throwable) {
        }
    }
}