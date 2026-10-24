package com.rexaps.rexpanel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import android.content.Context

class RexPanelViewModel : ViewModel() {

    var conn by mutableStateOf<ConnState>(
        ConnState.Idle
    )
        internal set

    var metrics by mutableStateOf<Metrics?>(null)
        internal set

    var files by mutableStateOf<FileBrowserState?>(null)
        internal set

    val cpuHistory =
        mutableStateListOf<Float>()

    val term =
        TermBuffer(80, 24)

    var ctrl by mutableStateOf(false)
        internal set

    var alt by mutableStateOf(false)
        internal set

    var shift by mutableStateOf(false)
        internal set

    internal var ssh: SshSession? = null
    internal var monitorJob: Job? = null
    internal var cols = 80
    internal var rows = 24

    internal val incoming =
        Channel<String>(Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            for (s in incoming) {
                term.feed(s)
            }
        }
    }

    fun toggleCtrl() {
        ctrl = !ctrl
    }

    fun toggleAlt() {
        alt = !alt
    }

    fun toggleShift() {
        shift = !shift
    }

    fun connect(
        command: String,
        password: String,
        elevate: Boolean,
        context: Context
    ) = connectImpl(command, password, elevate, context)

    fun disconnect() {

        monitorJob?.cancel()

        files?.close()
        files = null

        ssh?.close()
        ssh = null

        metrics = null

        conn =
            ConnState.Idle
    }

    fun resize(
        newCols: Int,
        newRows: Int
    ) = resizeImpl(newCols, newRows)

    fun sendRaw(s: String) {
        clearMods()

        ssh?.send(
            s.toByteArray(
                Charsets.UTF_8
            )
        )
    }

    fun sendText(text: String) = sendTextImpl(text)
    fun sendCursor(final: Char) = sendCursorImpl(final)

    /**
     * PgUp = 5
     * PgDn = 6
     * Delete = 3
     */
    fun sendTilde(n: Int) {
        sendRaw(
            "\u001B[$n~"
        )
    }

    override fun onCleared() {

        monitorJob?.cancel()

        files?.close()

        ssh?.close()

        super.onCleared()
    }
}
