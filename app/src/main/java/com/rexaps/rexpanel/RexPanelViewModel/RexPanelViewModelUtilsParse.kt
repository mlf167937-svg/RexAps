package com.rexaps.rexpanel

fun parseSample(raw: String): RawSample {
    val sec = HashMap<String, MutableList<String>>()
    var key = ""

    for (l in raw.lines()) {
        val t = l.trim()

        if (t.startsWith("##")) {
            key = t.removePrefix("##")
            sec[key] = mutableListOf()
        } else if (t.isNotEmpty()) {
            sec[key]?.add(t)
        }
    }

    fun lines(k: String): List<String> =
        sec[k].orEmpty()

    // CPU dari /proc/stat
    var cpuT: Long? = null
    var cpuI: Long? = null

    lines("STAT")
        .firstOrNull { it.startsWith("cpu ") }
        ?.let { line ->

            val n = line
                .split(WS)
                .drop(1)
                .mapNotNull { it.toLongOrNull() }

            if (n.size >= 5) {
                cpuT = n.take(8).sum()
                cpuI = n[3] + n[4]
            }
        }

    val topCpu =
        lines("TOP")
            .takeIf { it.isNotEmpty() }
            ?.let {
                parseTopCpu(it.joinToString("\n"))
            }

    // Frekuensi CPU
    val freqs = lines("FREQ").mapNotNull { l ->

        val t = l.split(WS)

        val cur = t
            .getOrNull(0)
            ?.toLongOrNull()

        val max = t
            .getOrNull(1)
            ?.toLongOrNull()

        if (cur != null && max != null && max > 0) {
            cur to max
        } else {
            null
        }
    }

    val freqFrac =
        if (freqs.isNotEmpty()) {
            (
                freqs.map { it.first }.average() /
                    freqs.maxOf { it.second }
                )
                .toFloat()
                .coerceIn(0f, 1f)
        } else {
            null
        }

    val freqMhz =
        if (freqs.isNotEmpty()) {
            (
                freqs.map { it.first }.average() / 1000
                ).toInt()
        } else {
            null
        }

    val cores =
        lines("CORES")
            .firstOrNull()
            ?.toIntOrNull()
            ?: lines("FREQ")
                .size
                .takeIf { it > 0 }

    // Memory
    var mt: Long? = null
    var ma: Long? = null

    for (l in lines("MEM")) {
        if (l.startsWith("MemTotal:")) {
            mt = l
                .filter { it.isDigit() }
                .toLongOrNull()
        }

        if (l.startsWith("MemAvailable:")) {
            ma = l
                .filter { it.isDigit() }
                .toLongOrNull()
        }
    }

    // Load
    val upCmd =
        lines("UPCMD")
            .joinToString(" ")

    val load =
        lines("LOAD")
            .firstOrNull { LOAD_RE.matches(it) }
            ?.split(" ")
            ?.take(3)
            ?.joinToString(" ")
            ?: UPCMD_LOAD_RE
                .find(upCmd)
                ?.let {
                    "${it.groupValues[1]} ${it.groupValues[2]} ${it.groupValues[3]}"
                }

    // Uptime
    val upSec =
        lines("UPTIME")
            .firstOrNull()
            ?.substringBefore('.')
            ?.toLongOrNull()

    val upText =
        UPCMD_UP_RE
            .find(upCmd)
            ?.groupValues
            ?.get(1)

    // Disk
    fun df(k: String): List<String>? =
        lines(k)
            .lastOrNull()
            ?.split(WS)
            ?.takeIf {
                it.size >= 6 &&
                    it[4].endsWith("%")
            }

    val d =
        df("DF")
            ?: df("DFH")

    // Network
    var rx = 0L
    var tx = 0L
    var hasNet = false

    for (l in lines("NET")) {
        if ('|' in l || ':' !in l) {
            continue
        }

        val name =
            l.substringBefore(':')
                .trim()

        if (name == "lo") {
            continue
        }

        val f =
            l.substringAfter(':')
                .trim()
                .split(WS)
                .mapNotNull { it.toLongOrNull() }

        if (f.size >= 9) {
            rx += f[0]
            tx += f[8]
            hasNet = true
        }
    }

    if (!hasNet) {
        for (l in lines("SYSNET")) {
            val t = l.split(WS)

            val a =
                t.getOrNull(1)
                    ?.toLongOrNull()

            val b =
                t.getOrNull(2)
                    ?.toLongOrNull()

            if (a != null && b != null) {
                rx += a
                tx += b
                hasNet = true
            }
        }
    }

    // Battery
    val bat =
        lines("BAT")
            .joinToString(" ")

    val batPct =
        Regex(""""percentage"\s*:\s*(\d+)""")
            .find(bat)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()

    val batStatus =
        Regex(""""status"\s*:\s*"(\w+)"""")
            .find(bat)
            ?.groupValues
            ?.get(1)

    val batTemp =
        Regex(""""temperature"\s*:\s*([\d.]+)""")
            .find(bat)
            ?.groupValues
            ?.get(1)
            ?.toFloatOrNull()

    return RawSample(
        cpuTotal = cpuT,
        cpuIdle = cpuI,

        freqFrac = freqFrac,
        freqMhz = freqMhz,
        cores = cores,

        memTotalKb = mt,
        memAvailKb = ma,

        load = load,

        uptimeSec = upSec,
        uptimeText = upText,

        diskPct =
            d?.get(4)
                ?.removeSuffix("%")
                ?.toIntOrNull(),

        diskUsedKb =
            d?.get(2)
                ?.toLongOrNull(),

        diskTotalKb =
            d?.get(1)
                ?.toLongOrNull(),

        diskMount =
            d?.drop(5)
                ?.joinToString(" "),

        rx =
            if (hasNet) rx else null,

        tx =
            if (hasNet) tx else null,

        batPct = batPct,
        batStatus = batStatus,
        batTemp = batTemp,

        host =
            lines("HOST")
                .firstOrNull(),

        topCpu = topCpu
    )
}
