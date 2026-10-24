package com.rexaps.rexpanel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
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
        private set

    var metrics by mutableStateOf<Metrics?>(null)
        private set

    var files by mutableStateOf<FileBrowserState?>(null)
        private set

    val cpuHistory =
        mutableStateListOf<Float>()

    val term =
        TermBuffer(80, 24)

    var ctrl by mutableStateOf(false)
        private set

    var alt by mutableStateOf(false)
        private set

    var shift by mutableStateOf(false)
        private set

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
    ) = this.__split_connect_1(command, password, elevate, context)

    internal fun onClosed(
        session: SshSession
    ) = this.__split_onClosed_2(session)

    internal fun startMonitor(
        session: SshSession,
        elevate: Boolean
    ) = this.__split_startMonitor_3(session, elevate)

    fun disconnect() = this.__split_disconnect_4()

    fun resize(
        newCols: Int,
        newRows: Int
    ) = this.__split_resize_5(newCols, newRows)

    /* ------------------------------- input ------------------------------ */

    private fun clearMods() {
        ctrl = false
        alt = false
        shift = false
    }

    fun sendRaw(s: String) {
        clearMods()

        ssh?.send(
            s.toByteArray(
                Charsets.UTF_8
            )
        )
    }

    fun sendText(text: String) = this.__split_sendText_6(text)

    /**
     * Panah, Home, End.
     *
     * final:
     * A = atas
     * B = bawah
     * C = kanan
     * D = kiri
     * H = home
     * F = end
     */
    fun sendCursor(final: Char) = this.__split_sendCursor_7(final)

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
