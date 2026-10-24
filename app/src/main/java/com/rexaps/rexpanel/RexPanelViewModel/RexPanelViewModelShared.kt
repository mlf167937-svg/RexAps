package com.rexaps.rexpanel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

internal fun __splitImpl_buildMetrics(
    cur: RawSample,
    prev: RawSample?
): Metrics {

    var cpu: Float? = null
    var rxBps: Long? = null
    var txBps: Long? = null

    if (prev != null) {

        // CPU
        val ct = cur.cpuTotal
        val ci = cur.cpuIdle

        val pt = prev.cpuTotal
        val pi = prev.cpuIdle

        if (
            ct != null &&
            ci != null &&
            pt != null &&
            pi != null
        ) {
            val dT = ct - pt
            val dI = ci - pi

            if (dT > 0) {
                cpu =
                    (
                        (dT - dI).toFloat() /
                            dT
                        )
                        .coerceIn(0f, 1f)
            }
        }

        // Network
        val secs =
            (cur.at - prev.at) / 1000.0

        val crx = cur.rx
        val ctx = cur.tx

        val prx = prev.rx
        val ptx = prev.tx

        if (
            secs > 0 &&
            crx != null &&
            ctx != null &&
            prx != null &&
            ptx != null
        ) {
            rxBps =
                ((crx - prx) / secs)
                    .toLong()
                    .coerceAtLeast(0)

            txBps =
                ((ctx - ptx) / secs)
                    .toLong()
                    .coerceAtLeast(0)
        }
    }

    if (cpu == null) {
        cpu = cur.topCpu
    }

    val total = cur.memTotalKb
    val avail = cur.memAvailKb

    return Metrics(
        host = cur.host ?: "server",

        cpu = cpu,

        cpuFreqFrac = cur.freqFrac,
        cpuFreqMhz = cur.freqMhz,

        cores = cur.cores,

        memUsedKb =
            if (
                total != null &&
                avail != null
            ) {
                total - avail
            } else {
                null
            },

        memTotalKb = total,

        load = cur.load,

        uptimeSec = cur.uptimeSec,
        uptimeText = cur.uptimeText,

        diskPct = cur.diskPct,
        diskUsedKb = cur.diskUsedKb,
        diskTotalKb = cur.diskTotalKb,
        diskMount = cur.diskMount,

        rxBps = rxBps,
        txBps = txBps,

        batteryPct = cur.batPct,
        batteryStatus = cur.batStatus,
        batteryTemp = cur.batTemp
    )
}

internal fun RexPanelViewModel.__splitImpl_connect(
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

internal fun RexPanelViewModel.__splitImpl_startMonitor(
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
