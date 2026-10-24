package com.rexaps.rexpanel

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

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
