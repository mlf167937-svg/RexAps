package com.rexaps.rexmanager

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rexaps.rexmanager.archive.ArchiveFormat
import com.rexaps.rexmanager.filesystem.FileOperationResult
import com.rexaps.rexmanager.filesystem.StorageInfo
import com.rexaps.rexmanager.filesystem.toFileError
import com.rexaps.rexmanager.settings.RexManagerPreferences
import com.rexaps.rexmanager.settings.RexManagerSettings
import com.rexaps.rexmanager.utils.FileNameUtils
import com.rexaps.rexmanager.utils.FileSorter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

class RexManagerViewModel(
    private val repository: RexManagerRepository,
    private val preferences: RexManagerPreferences
) : ViewModel(), RexManagerActions {

    private val _state = MutableStateFlow(RexManagerState(settings = preferences.settings.value))
    val state: StateFlow<RexManagerState> = _state.asStateFlow()

    private val _events = Channel<RexManagerEvent>(Channel.BUFFERED)
    val events: Flow<RexManagerEvent> = _events.receiveAsFlow()

    /** Raw listing of the current folder. */
    private var directoryFiles: List<RexFile> = emptyList()

    /** What the list currently derives from: [directoryFiles] or search results. */
    private var displayedFiles: List<RexFile> = emptyList()

    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var publishJob: Job? = null
    private var operationJob: Job? = null
    private var propertiesJob: Job? = null

    init {
        val roots = repository.storageRoots()
        _state.update { it.copy(roots = roots) }
        roots.firstOrNull()?.let { root ->
            _state.update { it.copy(currentRoot = root) }
            load(root.path)
        }
        viewModelScope.launch {
            preferences.settings.collect { new ->
                val old = _state.value.settings
                _state.update { it.copy(settings = new) }
                if (old != new) {
                    if (old.showHiddenFiles != new.showHiddenFiles && _state.value.isSearchActive) {
                        startSearch(_state.value.searchQuery, 0L)
                    } else {
                        republish()
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ loading

    private fun load(path: String) {
        searchJob?.cancel()
        loadJob?.cancel()
        _state.update {
            it.copy(
                currentPath = path,
                isLoading = true,
                errorMessage = null,
                selectedPaths = emptySet(),
                searchQuery = "",
                isSearchRunning = false,
                searchTruncated = false
            )
        }
        loadJob = viewModelScope.launch {
            when (val result = repository.list(path)) {
                is FileOperationResult.Success -> {
                    directoryFiles = result.value
                    displayedFiles = result.value
                    publish()
                    _state.update { it.copy(isLoading = false) }
                }
                is FileOperationResult.Failure -> {
                    directoryFiles = emptyList()
                    displayedFiles = emptyList()
                    _state.update {
                        it.copy(files = emptyList(), isLoading = false, errorMessage = result.error.message)
                    }
                }
            }
            refreshStorageInfo(path)
        }
    }

    private suspend fun refreshStorageInfo(path: String) {
        val info: StorageInfo? =
            (repository.storageInfo(path) as? FileOperationResult.Success)?.value
        _state.update { it.copy(storageInfo = info ?: it.storageInfo) }
    }

    private suspend fun publish() {
        val settings = _state.value.settings
        val source = displayedFiles
        val sorted = withContext(Dispatchers.Default) { FileSorter.filterAndSort(source, settings) }
        val visiblePaths = sorted.mapTo(HashSet()) { it.path }
        _state.update { it.copy(files = sorted, selectedPaths = it.selectedPaths.intersect(visiblePaths)) }
    }

    private fun republish() {
        publishJob?.cancel()
        publishJob = viewModelScope.launch { publish() }
    }

    private fun refresh() {
        val s = _state.value
        if (s.isSearchActive) startSearch(s.searchQuery, 0L) else load(s.currentPath)
    }

    private fun message(text: String) {
        _events.trySend(RexManagerEvent.Message(text))
    }

    // ------------------------------------------------------------------ navigation / search

    override fun onFileClick(file: RexFile) {
        val s = _state.value
        when {
            s.isSelectionMode -> toggleSelection(file.path)
            file.isDirectory -> load(file.path)
            ArchiveFormat.fromFileName(file.name) != null ->
                _state.update { it.copy(dialog = RexDialog.Extract(file)) }
            else -> _events.trySend(RexManagerEvent.OpenFile(file.path, file.mimeType))
        }
    }

    override fun onFileLongClick(file: RexFile) = toggleSelection(file.path)

    override fun onBreadcrumbClick(path: String) = load(path)

    override fun onSelectRoot(root: StorageRoot) {
        _state.update { it.copy(currentRoot = root) }
        load(root.path)
    }

    override fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            displayedFiles = directoryFiles
            _state.update { it.copy(isSearchRunning = false, searchTruncated = false) }
            republish()
            return
        }
        startSearch(query, 300L)
    }

    private fun startSearch(query: String, debounceMs: Long) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounceMs > 0L) delay(debounceMs)
            _state.update { it.copy(isSearchRunning = true) }
            val s = _state.value
            when (val r = repository.search(s.currentPath, query.trim(), s.settings.showHiddenFiles)) {
                is FileOperationResult.Success -> {
                    displayedFiles = r.value.files
                    publish()
                    _state.update {
                        it.copy(isSearchRunning = false, searchTruncated = r.value.truncated)
                    }
                }
                is FileOperationResult.Failure -> {
                    _state.update { it.copy(isSearchRunning = false) }
                    message(r.error.message)
                }
            }
        }
    }

    override fun onRefresh() = refresh()

    /** current folder -> parent folder -> storage root -> exit. */
    override fun onBackPressed(): Boolean {
        val s = _state.value
        when {
            s.showSettings -> {
                onCloseSettings()
                return true
            }
            s.isSelectionMode -> {
                onClearSelection()
                return true
            }
            s.isSearchActive -> {
                onSearchQueryChange("")
                return true
            }
        }
        val root = s.currentRoot ?: return false
        if (s.currentPath == root.path) return false
        val parent = File(s.currentPath).parent ?: return false
        load(if (parent.length < root.path.length) root.path else parent)
        return true
    }

    // ------------------------------------------------------------------ selection

    private fun toggleSelection(path: String) {
        _state.update { s ->
            s.copy(
                selectedPaths = if (path in s.selectedPaths) s.selectedPaths - path
                else s.selectedPaths + path
            )
        }
    }

    override fun onClearSelection() {
        _state.update { it.copy(selectedPaths = emptySet()) }
    }

    override fun onSelectAll() {
        _state.update { s -> s.copy(selectedPaths = s.files.mapTo(HashSet()) { it.path }) }
    }

    // ------------------------------------------------------------------ settings / view

    override fun onViewModeChange(mode: ViewMode) = preferences.update { it.copy(viewMode = mode) }
    override fun onSortModeChange(mode: SortMode) = preferences.update { it.copy(sortMode = mode) }
    override fun onSortOrderChange(order: SortOrder) = preferences.update { it.copy(sortOrder = order) }
    override fun onShowHiddenChange(show: Boolean) = preferences.update { it.copy(showHiddenFiles = show) }

    override fun onSettingsChange(transform: (RexManagerSettings) -> RexManagerSettings) =
        preferences.update(transform)

    override fun onOpenSettings() {
        _state.update { it.copy(showSettings = true) }
    }

    override fun onCloseSettings() {
        _state.update { it.copy(showSettings = false) }
    }

    // ------------------------------------------------------------------ dialog requests

    override fun onRequestCreate(isFolder: Boolean) {
        _state.update { it.copy(dialog = RexDialog.Create(isFolder)) }
    }

    override fun onRequestRename(file: RexFile) {
        _state.update { it.copy(dialog = RexDialog.Rename(file)) }
    }

    /** Deleting always asks for confirmation. */
    override fun onRequestDelete(files: List<RexFile>) {
        if (files.isEmpty()) return
        _state.update { it.copy(dialog = RexDialog.Delete(files)) }
    }

    override fun onRequestCompress(files: List<RexFile>) {
        if (files.isEmpty()) return
        _state.update { it.copy(dialog = RexDialog.Compress(files)) }
    }

    override fun onRequestExtract(archive: RexFile) {
        _state.update { it.copy(dialog = RexDialog.Extract(archive)) }
    }

    override fun onRequestProperties(file: RexFile) {
        propertiesJob?.cancel()
        propertiesJob = viewModelScope.launch {
            when (val r = repository.metadata(file.path)) {
                is FileOperationResult.Failure -> message(r.error.message)
                is FileOperationResult.Success -> {
                    val meta = r.value
                    _state.update {
                        it.copy(dialog = RexDialog.Properties(meta, if (meta.isDirectory) null else meta.size))
                    }
                    if (meta.isDirectory) {
                        val size = repository.totalSize(meta.path) ?: -1L
                        _state.update { s ->
                            val d = s.dialog
                            if (d is RexDialog.Properties && d.metadata.path == meta.path) {
                                s.copy(dialog = d.copy(totalSize = size))
                            } else {
                                s
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onRequestHide(files: List<RexFile>, hide: Boolean) {
        val targets = files.filter { it.name.startsWith(".") != hide }
        if (targets.isEmpty()) {
            message(if (hide) "The selected items are already hidden." else "The selected items are not hidden.")
            return
        }
        if (!_state.value.settings.confirmHideUnhide) {
            onConfirmHide(targets, hide)
            return
        }
        viewModelScope.launch {
            val conflicts = repository.countHideConflicts(targets.map { it.path }, hide)
            _state.update { it.copy(dialog = RexDialog.Hide(targets, hide, conflicts)) }
        }
    }

    override fun onDismissDialog() {
        propertiesJob?.cancel()
        _state.update { it.copy(dialog = null) }
    }

    // ------------------------------------------------------------------ share / clipboard

    override fun onShare(files: List<RexFile>) {
        val sendable = files.filter { !it.isDirectory }
        if (sendable.isEmpty()) {
            message("Folders can't be shared. Compress them first.")
            return
        }
        if (sendable.size < files.size) message("Folders were skipped. Compress them to share.")
        _events.trySend(RexManagerEvent.Share(sendable.map { it.path }))
        onClearSelection()
    }

    override fun onCopy(files: List<RexFile>) = setClipboard(files, isMove = false)

    override fun onMove(files: List<RexFile>) = setClipboard(files, isMove = true)

    private fun setClipboard(files: List<RexFile>, isMove: Boolean) {
        if (files.isEmpty()) return
        _state.update { it.copy(clipboard = Clipboard(files.map { f -> f.path }, isMove), selectedPaths = emptySet()) }
        message("Open the destination folder and tap Paste.")
    }

    override fun onCancelClipboard() {
        _state.update { it.copy(clipboard = null) }
    }

    override fun onPaste() {
        val s = _state.value
        val clip = s.clipboard ?: return
        val dest = s.currentPath
        val intoItself = clip.paths.any { dest == it || dest.startsWith(it + File.separator) }
        if (intoItself) {
            message("A folder can't be ${if (clip.isMove) "moved" else "copied"} into itself.")
            return
        }
        val transfer = PendingTransfer(clip.paths, dest, clip.isMove)
        viewModelScope.launch {
            val conflicts = repository.findConflicts(clip.paths, dest, clip.isMove)
            if (conflicts.isNotEmpty() && s.settings.confirmOverwrite) {
                _state.update { it.copy(dialog = RexDialog.Collision(transfer, conflicts)) }
            } else {
                startTransfer(transfer, CollisionPolicy.RENAME) // safe default: keep both
            }
        }
    }

    override fun onResolveCollision(transfer: PendingTransfer, policy: CollisionPolicy) =
        startTransfer(transfer, policy)

    private fun startTransfer(transfer: PendingTransfer, policy: CollisionPolicy) {
        _state.update { it.copy(clipboard = null, selectedPaths = emptySet(), dialog = null) }
        runOperation(if (transfer.isMove) "Moving" else "Copying") { report ->
            repository.transfer(transfer.paths, transfer.destination, transfer.isMove, policy, report)
                .toMessage(if (transfer.isMove) "Moved" else "Copied")
        }
    }

    // ------------------------------------------------------------------ confirmed actions

    private fun closeDialogAndSelection() {
        _state.update { it.copy(dialog = null, selectedPaths = emptySet()) }
    }

    override fun onConfirmCreate(isFolder: Boolean, name: String) {
        _state.update { it.copy(dialog = null) }
        val dir = _state.value.currentPath
        viewModelScope.launch {
            when (val r = repository.create(dir, name, isFolder)) {
                is FileOperationResult.Success -> {
                    message(if (isFolder) "Folder created." else "File created.")
                    refresh()
                }
                is FileOperationResult.Failure -> message(r.error.message)
            }
        }
    }

    override fun onConfirmRename(file: RexFile, newName: String) {
        closeDialogAndSelection()
        viewModelScope.launch {
            when (val r = repository.rename(file.path, newName)) {
                is FileOperationResult.Success -> refresh()
                is FileOperationResult.Failure -> message(r.error.message)
            }
        }
    }

    override fun onConfirmDelete(files: List<RexFile>) {
        closeDialogAndSelection()
        runOperation("Deleting") { report ->
            repository.delete(files.map { it.path }, report).toMessage("Deleted")
        }
    }

    override fun onConfirmCompress(files: List<RexFile>, name: String, format: ArchiveFormat) {
        closeDialogAndSelection()
        val dest = _state.value.currentPath
        runOperation("Compressing") { report ->
            when (val r = repository.compress(files.map { it.path }, dest, name, format, report)) {
                is FileOperationResult.Success -> "Created ${File(r.value).name}"
                is FileOperationResult.Failure -> r.error.message
            }
        }
    }

    override fun onConfirmExtract(archive: RexFile, folderName: String, policy: CollisionPolicy) {
        closeDialogAndSelection()
        val parent = File(archive.path).parent ?: _state.value.currentPath
        val dest = FileNameUtils.join(parent, folderName)
        runOperation("Extracting") { report ->
            when (val r = repository.extract(archive.path, dest, policy, report)) {
                is FileOperationResult.Success -> "Extracted ${r.value} file(s) to \"$folderName\""
                is FileOperationResult.Failure -> r.error.message
            }
        }
    }

    override fun onConfirmHide(files: List<RexFile>, hide: Boolean) {
        closeDialogAndSelection()
        runOperation(if (hide) "Hiding" else "Unhiding") { report ->
            repository.setHidden(files.map { it.path }, hide, report)
                .toMessage(if (hide) "Hidden" else "Unhidden")
        }
    }

    // ------------------------------------------------------------------ operation runner

    override fun onCancelOperation() {
        operationJob?.cancel()
    }

    /** Runs one long operation at a time, with progress, cancellation and error reporting. */
    private fun runOperation(title: String, block: suspend (ProgressReporter) -> String?) {
        if (operationJob?.isActive == true) {
            message("Another operation is still running.")
            return
        }
        operationJob = viewModelScope.launch {
            _state.update { it.copy(operationProgress = OperationProgress(title)) }
            try {
                val result = block { label, fraction ->
                    _state.update { s ->
                        s.copy(operationProgress = s.operationProgress?.copy(currentName = label, fraction = fraction))
                    }
                }
                result?.let { message(it) }
            } catch (e: CancellationException) {
                message("Operation cancelled.")
                throw e
            } catch (e: Exception) {
                message(e.toFileError().message)
            } finally {
                _state.update { it.copy(operationProgress = null) }
                refresh()
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RexManagerViewModel(
                        repository = RexManagerRepository(appContext),
                        preferences = RexManagerPreferences(appContext)
                    ) as T
            }
        }
    }
}
