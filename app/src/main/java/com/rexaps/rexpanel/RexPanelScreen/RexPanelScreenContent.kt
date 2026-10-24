package com.rexaps.rexpanel

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun MonitorSection(
    m: Metrics?,
    cpuHistory: List<Float>
) {
    Column(
        Modifier.padding(
            horizontal = 16.dp,
            vertical = 8.dp
        ),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        if (m == null) {

            LinearProgressIndicator(
                Modifier.fillMaxWidth()
            )

            Text(
                "Mengambil data server…",
                style =
                    MaterialTheme.typography
                        .bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }

        val total =
            m?.memTotalKb

        val used =
            m?.memUsedKb

        val memFrac =
            if (
                total != null &&
                used != null &&
                total > 0
            ) {
                used.toFloat() /
                    total
            } else {
                null
            }

        val cpuFrac =
            m?.cpu ?: m?.cpuFreqFrac

        val cpuValue =
            when {
                m?.cpu != null ->
                    "${(m.cpu * 100).toInt()}%"

                m?.cpuFreqMhz != null ->
                    fmtMhz(m.cpuFreqMhz)

                else ->
                    "–"
            }

        val coreText =
            m?.cores?.let {
                " · $it core"
            } ?: ""

        val cpuSub =
            when {
                m?.cpu != null ->
                    "Penggunaan$coreText"

                m?.cpuFreqMhz != null ->
                    "Frekuensi$coreText"

                else ->
                    "Tidak tersedia"
            }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            GaugeCard(
                title = "CPU",
                fraction = cpuFrac,
                value = cpuValue,
                sub = cpuSub,
                history = cpuHistory,
                modifier =
                    Modifier.weight(1f)
            )

            GaugeCard(
                title = "RAM",
                fraction = memFrac,
                value =
                    memFrac?.let {
                        "${(it * 100).toInt()}%"
                    } ?: "–",
                sub =
                    if (
                        used != null &&
                        total != null
                    ) {
                        "${fmtKb(used)} / ${fmtKb(total)}"
                    } else {
                        "Tidak tersedia"
                    },
                history = null,
                modifier =
                    Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            StatTile(
                "Disk ${m?.diskMount ?: ""}".trim(),
                m?.diskPct?.let {
                    "$it%"
                } ?: "–",
                if (
                    m?.diskUsedKb != null &&
                    m.diskTotalKb != null
                ) {
                    "${fmtKb(m.diskUsedKb)} / ${fmtKb(m.diskTotalKb)}"
                } else {
                    ""
                },
                Modifier.weight(1f)
            )

            StatTile(
                "Uptime",
                m?.uptimeSec?.let {
                    fmtUptime(it)
                } ?: m?.uptimeText ?: "–",
                "",
                Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            StatTile(
                "Load",
                m?.load
                    ?.substringBefore(' ')
                    ?: "–",
                m?.load ?: "",
                Modifier.weight(1f)
            )

            StatTile(
                "Jaringan",
                "↓ " +
                    (
                        m?.rxBps?.let {
                            fmtRate(it)
                        } ?: "–"
                        ),
                "↑ " +
                    (
                        m?.txBps?.let {
                            fmtRate(it)
                        } ?: "–"
                        ),
                Modifier.weight(1f)
            )
        }

        if (m?.batteryPct != null) {

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                StatTile(
                    "Baterai",
                    "${m.batteryPct}%",
                    m.batteryStatus
                        ?.lowercase()
                        ?.replaceFirstChar {
                            it.uppercase()
                        } ?: "",
                    Modifier.weight(1f)
                )

                StatTile(
                    "Suhu",
                    m.batteryTemp?.let {
                        String.format(
                            "%.1f°C",
                            it
                        )
                    } ?: "–",
                    "Baterai",
                    Modifier.weight(1f)
                )
            }
        }

        if (
            m != null &&
            m.cpu == null &&
            m.rxBps == null
        ) {

            Text(
                "Sebagian data tidak tersedia karena akun ini bukan root. Itu normal; terminal dan file tetap berfungsi.",
                style =
                    MaterialTheme.typography
                        .labelSmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}
