
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope


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
        return __splitImpl_HomeHeader(state, onRefresh, onGithub, compact)
    }

internal fun accountSuffix(state: RexGitUiState): String {
        return __splitImpl_accountSuffix(state)
    }

/** Location text used by the "new file / folder" sheets and dialogs. */
internal fun locationLabel(state: RexGitUiState): String {
        return __splitImpl_locationLabel(state)
    }

@Composable
internal fun RepositoryHeader(
    repo: RexGitRepository,
    busy: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    compact: Boolean = false
) {
        return __splitImpl_RepositoryHeader(repo, busy, onBack, onRefresh, compact)
    }

/** Branch / clean / changes pills (same as before). */
@Composable
internal fun RepositoryStatusPills(
    state: RexGitUiState,
    repo: RexGitRepository,
    changes: List<RexGitChange>
) {
        return __splitImpl_RepositoryStatusPills(state, repo, changes)
    }

/** GitDev + account pills. Tapping them opens the matching dialog. */
@Composable
internal fun RepositoryToolPills(
    state: RexGitUiState,
    repo: RexGitRepository,
    onGitDev: () -> Unit,
    onAccount: () -> Unit
) {
        return __splitImpl_RepositoryToolPills(state, repo, onGitDev, onAccount)
    }

@Composable
internal fun TappablePill(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
        return __splitImpl_TappablePill(onClick, content)
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
        return __splitImpl_RepositoryFiles(state, repo, onOpen, onMore, onNavigate, onNew, modifier)
    }

/** Pull / Push bar. Standard Push is available even when GitDev is disabled. */
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
        return __splitImpl_ActionBar(isGit, busy, pulling, pushing, changeCount, gitDevReady, onPull, onPush, onUnlockPush)
    }

@Composable
internal fun Crumb(
    text: String,
    active: Boolean,
    onClick: () -> Unit
) {
        return __splitImpl_Crumb(text, active, onClick)
    }

@Composable
internal fun ChangesPanel(
    changes: List<RexGitChange>,
    expanded: Boolean,
    onToggle: () -> Unit
) {
        return __splitImpl_ChangesPanel(changes, expanded, onToggle)
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
        return __splitImpl_CreateSheet(location, onDismiss, onPick)
    }

@Composable
internal fun ItemActionsSheet(
    file: RexGitFile,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
        return __splitImpl_ItemActionsSheet(file, onDismiss, onOpen, onDelete)
    }

@Composable
internal fun SheetAction(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
        return __splitImpl_SheetAction(icon, title, subtitle, onClick, destructive)
    }

@Composable
internal fun NameDialog(
    kind: CreateKind,
    location: String,
    existing: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
        return __splitImpl_NameDialog(kind, location, existing, onDismiss, onConfirm)
    }

@Composable
internal fun DeleteDialog(
    file: RexGitFile,
    isGit: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
        return __splitImpl_DeleteDialog(file, isGit, onDismiss, onConfirm)
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
        return __splitImpl_WorkspaceCard(total, gitCount, path)
    }

@Composable
internal fun RepositoryCard(
    repo: RexGitRepository,
    onClick: () -> Unit
) {
        return __splitImpl_RepositoryCard(repo, onClick)
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
        return __splitImpl_GithubRepositoryCard(repo, cloned, cloning, anyCloning, onClone, onOpen)
    }

internal data class FileStyle(val icon: ImageVector, val color: Color)

internal fun fileStyle(file: RexGitFile, scheme: ColorScheme): FileStyle {
        return __splitImpl_fileStyle(file, scheme)
    }

@Composable
internal fun FileRow(
    file: RexGitFile,
    change: RexGitChangeKind?,
    hasChangesInside: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit
) {
        return __splitImpl_FileRow(file, change, hasChangesInside, onClick, onMore)
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
        return __splitImpl_RexGitDialogFrame(title, onDismiss, actions, content)
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
        return __splitImpl_AccountSwitcher(accounts, active, onSwitch, modifier)
    }

@Composable
internal fun AccountRow(
    name: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
        return __splitImpl_AccountRow(name, active, enabled, onClick, onRemove)
    }

@Composable
internal fun AccountForm(
    loading: Boolean,
    submitLabel: String,
    onSubmit: (username: String, token: String) -> Unit,
    onCancel: (() -> Unit)?
) {
        return __splitImpl_AccountForm(loading, submitLabel, onSubmit, onCancel)
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
        return __splitImpl_GithubDialog(state, onDismiss, onLoadEnv, onAdd, onSwitch, onRemove, onDisconnect)
    }

// ============================================================
// GitDev mode: guide + setup
// ============================================================

@Composable
internal fun GuideHeading(text: String) {
        return __splitImpl_GuideHeading(text)
    }

@Composable
internal fun GuideText(text: String) {
        return __splitImpl_GuideText(text)
    }

@Composable
internal fun GuideCode(text: String) {
        return __splitImpl_GuideCode(text)
    }

@Composable
internal fun GitDevGuide() {
        return __splitImpl_GitDevGuide()
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
        return __splitImpl_GitDevDialog(repo, config, onSave, onDismiss)
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
    useGitDev: Boolean,
    gitDevAvailable: Boolean,
    onUseGitDevChange: (Boolean) -> Unit,
    onFolderChange: (String) -> Unit,
    bumpVersion: Boolean,
    onBumpChange: (Boolean) -> Unit,
    changeCount: Int,
    onDismiss: () -> Unit,
    onPush: () -> Unit
) {
        return __splitImpl_PushDialog(message, onMessageChange, type, onTypeChange, folder, folders, packagePath, useGitDev, gitDevAvailable, onUseGitDevChange, onFolderChange, bumpVersion, onBumpChange, changeCount, onDismiss, onPush)
    }

@Composable
internal fun RexPushProgressDialog(
    state: RexGitUiState,
    onDismiss: () -> Unit
) {
        return __splitImpl_RexPushProgressDialog(state, onDismiss)
    }

@Composable
internal fun RexPushLoader(active: Boolean) {
        return __splitImpl_RexPushLoader(active)
    }

@Composable
internal fun RexPushSuccessAnimation() {
        return __splitImpl_RexPushSuccessAnimation()
    }

@Composable
internal fun ErrorDialog(
    text: String,
    onDismiss: () -> Unit
) {
        return __splitImpl_ErrorDialog(text, onDismiss)
    }

// ============================================================
// Shared building blocks
// ============================================================

@Composable
internal fun RexGitLogo() {
        return __splitImpl_RexGitLogo()
    }

@Composable
internal fun IconTile(
    icon: ImageVector,
    tint: Color,
    size: Dp = 40.dp
) {
        return __splitImpl_IconTile(icon, tint, size)
    }

@Composable
internal fun HeaderIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
        return __splitImpl_HeaderIconButton(icon, description, onClick, enabled, tint)
    }

@Composable
internal fun SegmentedTabs(
    labels: List<String>,
    icons: List<ImageVector>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
        return __splitImpl_SegmentedTabs(labels, icons, selected, onSelect)
    }

/** Reserves space for the progress bar so the layout never jumps. */
@Composable
internal fun ProgressSlot(visible: Boolean) {
        return __splitImpl_ProgressSlot(visible)
    }

@Composable
internal fun ButtonSpinner() {
        return __splitImpl_ButtonSpinner()
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
        return __splitImpl_GradientButton(text, icon, onClick, modifier, enabled, loading)
    }

@Composable
internal fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null
) {
        return __splitImpl_SearchField(value, onValueChange, placeholder, modifier, onSearch)
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
        return __splitImpl_EmptyState(icon, title, body, modifier, actionLabel, onAction)
    }

@Composable
internal fun StorageNotice(
    path: String,
    onGrant: () -> Unit
) {
        return __splitImpl_StorageNotice(path, onGrant)
    }

@Composable
internal fun InfoPill(
    icon: ImageVector,
    text: String,
    container: Color,
    content: Color,
    compact: Boolean = false
) {
        return __splitImpl_InfoPill(icon, text, container, content, compact)
    }

@Composable
internal fun ChangeBadge(kind: RexGitChangeKind) {
        return __splitImpl_ChangeBadge(kind)
    }

// ============================================================
// Utilities
// ============================================================

internal fun formatSize(bytes: Long): String {
        return __splitImpl_formatSize(bytes)
    }

internal fun openAllFilesAccess(context: Context) {
        return __splitImpl_openAllFilesAccess(context)
    }