package com.rexaps.rexpanel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal fun FileBrowserState.__splitImpl_deleteAll(list: List<RemoteFile>) {
        scope.launch {
            var ok = 0
            var fail = 0
            var last: String? = null
            for (f in list) {
                try {
                    sftp.remove(f)
                    ok++
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    fail++
                    last = sftpMessage(e)
                }
            }
            notice = if (fail == 0) "$ok item dihapus" else "$ok dihapus, $fail gagal" + (last?.let { ": $it" } ?: "")
            refresh()
        }
    }
