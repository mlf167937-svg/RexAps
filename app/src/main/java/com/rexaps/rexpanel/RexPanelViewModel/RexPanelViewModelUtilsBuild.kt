package com.rexaps.rexpanel

internal fun buildMetrics(
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

internal fun friendly(e: Exception): String {
    val m = e.message.orEmpty()
    val l = m.lowercase()

    return when {
        "auth" in l ->
            "Login ditolak. Cek user dan password."

        "refused" in l ->
            "Koneksi ditolak. Cek port dan pastikan SSH server berjalan."

        "timeout" in l ||
            "timed out" in l ->
            "Server tidak merespons. Cek IP, port, dan jaringan."

        "unknownhost" in l ->
            "Host tidak ditemukan."

        else ->
            m.ifBlank {
                "Gagal terhubung."
            }
    }
}
