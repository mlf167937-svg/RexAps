package com.rexaps.rexpanel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import android.content.Context

fun RexPanelViewModel.__split_connect_1(
        command: String,
        password: String,
        elevate: Boolean,
        context: Context
    ) {
        val target =
            parseSshCommand(command)

        if (target == null) {
            conn =
                ConnState.Error(
                    "Format salah. Tulis user-nya, contoh: ssh -p 8022 u0_a123@192.168.0.101"
                )
            return
        }

        if (conn is ConnState.Connecting) {
            return
        }

        conn = ConnState.Connecting

        term.reset()

        viewModelScope.launch {

            val session = SshSession()

            try {

                session.connect(
                    target = target,
                    password = password,
                    cols = cols,
                    rows = rows,

                    onText = {
                        incoming.trySend(it)
                    },

                    onClosed = {
                        viewModelScope.launch {
                            onClosed(session)
                        }
                    }
                )

                ssh = session

                val client =
                    SftpClient {
                        session.openSftp()
                    }

                val browser =
                    FileBrowserState(
                        client,
                        viewModelScope,
                        { cmd ->
                            session.exec(cmd)
                        },
                        context.cacheDir,
                        context
                    )

                files = browser

                conn =
                    ConnState.Connected(target)

                browser.start()

                startMonitor(
                    session,
                    elevate
                )

            } catch (e: Exception) {

                session.close()

                files?.close()
                files = null

                conn =
                    ConnState.Error(
                        friendly(e)
                    )
            }
        }
    }

internal fun RexPanelViewModel.__split_onClosed_2(
        session: SshSession
    ) {
        if (ssh === session) {

            monitorJob?.cancel()

            files?.close()
            files = null

            ssh = null

            metrics = null

            conn =
                ConnState.Error(
                    "Koneksi terputus."
                )
        }
    }

internal fun RexPanelViewModel.__split_startMonitor_3(
        session: SshSession,
        elevate: Boolean
    ) {

        monitorJob?.cancel()

        cpuHistory.clear()

        val cmd =
            monCmd(elevate)

        monitorJob =
            viewModelScope.launch {

                var prev: RawSample? = null

                while (isActive) {

                    try {

                        val raw =
                            withTimeoutOrNull(8_000) {
                                session.exec(cmd)
                            }

                        if (raw != null) {

                            val sample =
                                parseSample(raw)

                            val m =
                                buildMetrics(
                                    sample,
                                    prev
                                )

                            metrics = m

                            (
                                m.cpu
                                    ?: m.cpuFreqFrac
                                )?.let {

                                    cpuHistory.add(it)

                                    if (cpuHistory.size > 40) {
                                        cpuHistory.removeAt(0)
                                    }
                                }

                            prev = sample
                        }

                    } catch (e: CancellationException) {
                        throw e

                    } catch (_: Exception) {
                        // Monitor harus tetap berjalan
                        // meskipun satu sample gagal.
                    }

                    delay(2_000)
                }
            }
    }

fun RexPanelViewModel.__split_disconnect_4() {

        monitorJob?.cancel()

        files?.close()
        files = null

        ssh?.close()
        ssh = null

        metrics = null

        conn =
            ConnState.Idle
    }

fun RexPanelViewModel.__split_sendText_6(text: String) {

        if (text.isEmpty()) {
            return
        }

        var s = text
        val c = s[0]

        if (ctrl) {

            val lc =
                c.lowercaseChar()

            val code =
                when {
                    lc in 'a'..'z' ->
                        lc.code - 96

                    c == '[' ->
                        27

                    c == '\\' ->
                        28

                    c == ']' ->
                        29

                    c == '^' ->
                        30

                    c == '_' ->
                        31

                    c == ' ' ||
                        c == '@' ->
                        0

                    else ->
                        -1
                }

            if (code >= 0) {
                s =
                    code.toChar().toString() +
                        s.drop(1)
            }

        } else if (shift) {

            s =
                c.uppercaseChar().toString() +
                    s.drop(1)
        }

        if (alt) {
            s =
                "\u001B" + s
        }

        sendRaw(s)
    }
