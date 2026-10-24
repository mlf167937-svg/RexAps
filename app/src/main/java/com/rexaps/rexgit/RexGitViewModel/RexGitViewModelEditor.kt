package com.rexaps.rexgit

import kotlinx.coroutines.flow.update
import java.io.File

// ---------- editor ----------

/**
 * Latest editor text. The UI reads it when the layout switches between
 * portrait and landscape so unsaved typing is never lost.
 */
internal fun RexGitViewModel.editorTextImpl(): String = editorBuffer

internal fun RexGitViewModel.updateEditorImpl(content: String) {
    editorBuffer = content

    _state.update {
        if (it.editorDirty) {
            it
        } else {
            it.copy(editorDirty = true)
        }
    }
}

internal fun RexGitViewModel.closeEditorImpl() {
    editorBuffer = ""

    _state.update {
        it.copy(
            editorPath = null,
            editorContent = "",
            editorDirty = false
        )
    }
}

internal fun RexGitViewModel.saveEditorImpl() {
    val path = _state.value.editorPath
        ?: return

    if (_state.value.isSaving) return

    val content = editorBuffer

    launchIO {
        _state.update {
            it.copy(isSaving = true)
        }

        try {
            File(path).writeText(content)

            val repo = _state.value.selectedRepository

            val status =
                if (repo != null && repo.isGitRepository) {
                    repositoryManager.status(repo)
                } else {
                    null
                }

            _state.update {
                it.copy(
                    editorDirty = editorBuffer != content,
                    gitStatus = status ?: it.gitStatus,
                    readmeContent = if (File(path).name.equals("README.md", true)) content else it.readmeContent,
                    message = "File saved"
                )
            }
        } finally {
            _state.update {
                it.copy(isSaving = false)
            }
        }
    }
}
