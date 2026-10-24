package com.rexaps.rexgit

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------- helpers ----------

/** Runs on IO and converts ANY throwable (including Errors) into a UI error instead of a crash. */
internal fun RexGitViewModel.launchIO(block: suspend () -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            fail(t.readableMessage("Unexpected error"))
        }
    }
}

internal fun RexGitViewModel.fail(text: String) {
    _state.update {
        it.copy(
            error = text,
            message = null
        )
    }
}

internal fun RexGitViewModel.storageInfo(): Pair<String, Boolean> {
    return RexGitStorage.githubRoot.absolutePath to RexGitStorage.usingFallback
}
