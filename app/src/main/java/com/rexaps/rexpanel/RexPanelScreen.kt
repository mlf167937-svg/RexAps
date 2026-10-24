package com.rexaps.rexpanel

import android.app.Activity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp

internal val TermFg = Color(0xFFCDD6F4)
internal val Good = Color(0xFFA6E3A1)
internal val Warn = Color(0xFFF9E2AF)
internal val Bad = Color(0xFFF38BA8)

internal enum class PanelTab(
    val label: String,
    val icon: ImageVector
) {
    Dashboard("Dasbor", Icons.Default.Dashboard),
    Terminal("Terminal", Icons.Default.Terminal),
    Files("File", Icons.Default.Folder)
}

@Composable
fun RexPanelScreen(
    activity: Activity,
    onExit: () -> Unit
) {
        return __splitImpl_RexPanelScreen(activity, onExit)
    }

/* -------------------------------- CONNECT -------------------------------- */

@Composable
internal fun ConnectForm(
    state: ConnState,
    onConnect: (
        String,
        String,
        Boolean
    ) -> Unit,
    onExit: () -> Unit
) {
        return __splitImpl_ConnectForm(state, onConnect, onExit)
    }

@Composable
internal fun PresetChip(
    label: String,
    onClick: () -> Unit
) {
    val colors =
        MaterialTheme.colorScheme

    val shape =
        RoundedCornerShape(12.dp)

    Box(
        Modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .border(
                1.dp,
                colors.outlineVariant,
                shape
            )
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .padding(
                horizontal = 14.dp
            ),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            label,
            style =
                MaterialTheme.typography
                    .labelLarge
        )
    }
}

/* -------------------------------- HEADER --------------------------------- */

@Composable
internal fun PanelHeader(
    target: SshTarget,
    host: String?,
    onMinimize: () -> Unit,
    onDisconnect: () -> Unit
) {
        return __splitImpl_PanelHeader(target, host, onMinimize, onDisconnect)
    }

@Composable
internal fun PanelTabs(
    selected: PanelTab,
    onSelect: (PanelTab) -> Unit
) {
        return __splitImpl_PanelTabs(selected, onSelect)
    }

/* -------------------------------- MONITOR -------------------------------- */

@Composable
internal fun MonitorSection(
    m: Metrics?,
    cpuHistory: List<Float>
) {
        return __splitImpl_MonitorSection(m, cpuHistory)
    }

internal fun fmtMhz(
    mhz: Int
): String =
    if (mhz >= 1000) {
        String.format(
            "%.1f GHz",
            mhz / 1000.0
        )
    } else {
        "$mhz MHz"
    }

@Composable
internal fun GaugeCard(
    title: String,
    fraction: Float?,
    value: String,
    sub: String,
    history: List<Float>?,
    modifier: Modifier
) {
        return __splitImpl_GaugeCard(title, fraction, value, sub, history, modifier)
    }

@Composable
internal fun StatTile(
    title: String,
    value: String,
    sub: String,
    modifier: Modifier
) {
        return __splitImpl_StatTile(title, value, sub, modifier)
    }

internal fun fmtKb(
    kb: Long
): String {
    val mb =
        kb / 1024.0

    return if (mb >= 1024) {
        String.format(
            "%.1f GB",
            mb / 1024
        )
    } else {
        String.format(
            "%.0f MB",
            mb
        )
    }
}

internal fun fmtRate(
    bps: Long
): String =
    when {
        bps >= 1_048_576 ->
            String.format(
                "%.1f MB/s",
                bps / 1_048_576.0
            )

        bps >= 1024 ->
            String.format(
                "%.0f KB/s",
                bps / 1024.0
            )

        else ->
            "$bps B/s"
    }

internal fun fmtUptime(
    sec: Long
): String {

    val d =
        sec / 86_400

    val h =
        sec % 86_400 / 3600

    val m =
        sec % 3600 / 60

    return when {
        d > 0 ->
            "${d}h ${h}j"

        h > 0 ->
            "${h}j ${m}m"

        else ->
            "${m}m"
    }
}

/* -------------------------------- TERMINAL -------------------------------- */

@Composable
internal fun TerminalPane(
    vm: RexPanelViewModel,
    fontSize: Int,
    fullscreen: Boolean,
    onToggleFull: () -> Unit,
    onFontSize: (Int) -> Unit,
    modifier: Modifier
) {
        return __splitImpl_TerminalPane(vm, fontSize, fullscreen, onToggleFull, onFontSize, modifier)
    }

internal fun handleKey(
    e: KeyEvent,
    vm: RexPanelViewModel
): Boolean {

    if (
        e.type !=
            KeyEventType.KeyDown
    ) {
        return false
    }

    when (e.key) {

        Key.Enter,
        Key.NumPadEnter ->
            vm.sendRaw("\r")

        Key.Backspace ->
            vm.sendRaw("\u007F")

        Key.Tab ->
            vm.sendRaw("\t")

        Key.Escape ->
            vm.sendRaw("\u001B")

        Key.DirectionUp ->
            vm.sendCursor('A')

        Key.DirectionDown ->
            vm.sendCursor('B')

        Key.DirectionRight ->
            vm.sendCursor('C')

        Key.DirectionLeft ->
            vm.sendCursor('D')

        else ->
            return false
    }

    return true
}

/* --------------------------------- KEYBAR --------------------------------- */

@Composable
internal fun KeyBar(
    vm: RexPanelViewModel
) {
        return __splitImpl_KeyBar(vm)
    }

@Composable
internal fun KeyChip(
    label: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
        return __splitImpl_KeyChip(label, active, onClick)
    }