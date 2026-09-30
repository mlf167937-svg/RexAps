package com.rexaps.rexmanager

import com.rexaps.rexmanager.archive.ArchiveFormat
import com.rexaps.rexmanager.filesystem.FileMetadata
import com.rexaps.rexmanager.filesystem.StorageInfo
import com.rexaps.rexmanager.settings.RexManagerSettings

data class RexManagerState(
    val roots: List<StorageRoot> = emptyList(),
    val currentRoot: StorageRoot? = null,
    val currentPath: String = "",
    val files: List<RexFile> = emptyList(),
    val selectedPaths: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val isSearchRunning: Boolean = false,
    val searchTruncated: Boolean = false,
    val settings: RexManagerSettings = RexManagerSettings(),
    val storageInfo: StorageInfo? = null,
    val clipboard: Clipboard? = null,
    val operationProgress: OperationProgress? = null,
    val dialog: RexDialog? = null,
    val showSettings: Boolean = false
) {
    val showHiddenFiles: Boolean get() = settings.showHiddenFiles
    val viewMode: ViewMode get() = settings.viewMode
    val sortMode: SortMode get() = settings.sortMode
    val sortOrder: SortOrder get() = settings.sortOrder
    val isSelectionMode: Boolean get() = selectedPaths.isNotEmpty()
    val isSearchActive: Boolean get() = searchQuery.isNotBlank()
}

sealed interface RexDialog {
    data class Rename(val file: RexFile) : RexDialog
    data class Create(val isFolder: Boolean) : RexDialog
    data class Delete(val files: List<RexFile>) : RexDialog
    data class Compress(val files: List<RexFile>) : RexDialog
    data class Extract(val archive: RexFile) : RexDialog

    /** totalSize: null = still calculating, negative = unavailable. */
    data class Properties(val metadata: FileMetadata, val totalSize: Long?) : RexDialog
    data class Hide(val files: List<RexFile>, val hide: Boolean, val conflicts: Int) : RexDialog
    data class Collision(val transfer: PendingTransfer, val conflicts: List<String>) : RexDialog
}

/** One-shot events that must be handled by the Android layer (Snackbar, Intents). */
sealed interface RexManagerEvent {
    data class Message(val text: String) : RexManagerEvent
    data class Share(val paths: List<String>) : RexManagerEvent
    data class OpenFile(val path: String, val mimeType: String?) : RexManagerEvent
}

/** UI -> ViewModel contract. Keeps composables stateless. */
interface RexManagerActions {
    fun onFileClick(file: RexFile)
    fun onFileLongClick(file: RexFile)
    fun onBreadcrumbClick(path: String)
    fun onSelectRoot(root: StorageRoot)
    fun onSearchQueryChange(query: String)
    fun onRefresh()

    /** Returns true if the back press was consumed, false if RexManager should exit. */
    fun onBackPressed(): Boolean

    fun onClearSelection()
    fun onSelectAll()

    fun onViewModeChange(mode: ViewMode)
    fun onSortModeChange(mode: SortMode)
    fun onSortOrderChange(order: SortOrder)
    fun onShowHiddenChange(show: Boolean)
    fun onSettingsChange(transform: (RexManagerSettings) -> RexManagerSettings)
    fun onOpenSettings()
    fun onCloseSettings()

    fun onRequestCreate(isFolder: Boolean)
    fun onRequestRename(file: RexFile)
    fun onRequestDelete(files: List<RexFile>)
    fun onRequestCompress(files: List<RexFile>)
    fun onRequestExtract(archive: RexFile)
    fun onRequestProperties(file: RexFile)
    fun onRequestHide(files: List<RexFile>, hide: Boolean)
    fun onShare(files: List<RexFile>)
    fun onCopy(files: List<RexFile>)
    fun onMove(files: List<RexFile>)
    fun onPaste()
    fun onCancelClipboard()

    fun onConfirmCreate(isFolder: Boolean, name: String)
    fun onConfirmRename(file: RexFile, newName: String)
    fun onConfirmDelete(files: List<RexFile>)
    fun onConfirmCompress(files: List<RexFile>, name: String, format: ArchiveFormat)
    fun onConfirmExtract(archive: RexFile, folderName: String, policy: CollisionPolicy)
    fun onConfirmHide(files: List<RexFile>, hide: Boolean)
    fun onResolveCollision(transfer: PendingTransfer, policy: CollisionPolicy)
    fun onDismissDialog()
    fun onCancelOperation()
}
