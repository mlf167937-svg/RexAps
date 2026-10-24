package com.rexaps.rexpanel

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rexaps.rexfox.rexPressable

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

/* --------------------------------- KEYBAR --------------------------------- */

@Composable
internal fun KeyBar(
    vm: RexPanelViewModel
) {
    val colors =
        MaterialTheme.colorScheme

    Row(
        Modifier
            .fillMaxWidth()
            .background(
                colors.surfaceVariant
            )
            .horizontalScroll(
                rememberScrollState()
            )
            .padding(
                horizontal = 8.dp,
                vertical = 6.dp
            ),

        horizontalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {

        KeyChip(
            "Ctrl",
            vm.ctrl
        ) {
            vm.toggleCtrl()
        }

        KeyChip(
            "Alt",
            vm.alt
        ) {
            vm.toggleAlt()
        }

        KeyChip(
            "Shift",
            vm.shift
        ) {
            vm.toggleShift()
        }

        KeyChip("Esc") {
            vm.sendRaw("\u001B")
        }

        KeyChip("Tab") {
            vm.sendRaw("\t")
        }

        KeyChip("←") {
            vm.sendCursor('D')
        }

        KeyChip("↑") {
            vm.sendCursor('A')
        }

        KeyChip("↓") {
            vm.sendCursor('B')
        }

        KeyChip("→") {
            vm.sendCursor('C')
        }

        KeyChip("Home") {
            vm.sendCursor('H')
        }

        KeyChip("End") {
            vm.sendCursor('F')
        }

        KeyChip("PgUp") {
            vm.sendTilde(5)
        }

        KeyChip("PgDn") {
            vm.sendTilde(6)
        }

        KeyChip("Del") {
            vm.sendTilde(3)
        }

        KeyChip("^C") {
            vm.sendRaw("\u0003")
        }

        KeyChip("^D") {
            vm.sendRaw("\u0004")
        }

        KeyChip("^Z") {
            vm.sendRaw("\u001A")
        }

        KeyChip("-") {
            vm.sendText("-")
        }

        KeyChip("/") {
            vm.sendText("/")
        }

        KeyChip("|") {
            vm.sendText("|")
        }

        KeyChip("~") {
            vm.sendText("~")
        }
    }
}

@Composable
internal fun KeyChip(
    label: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val colors =
        MaterialTheme.colorScheme

    val shape =
        RoundedCornerShape(12.dp)

    Box(
        Modifier
            .height(44.dp)
            .widthIn(min = 44.dp)
            .semantics {
                role = Role.Button
                selected = active
            }
            .rexPressable(
                onClick = onClick
            )
            .clip(shape)
            .background(
                if (active) {
                    colors.primary
                } else {
                    colors.background
                }
            )
            .border(
                1.dp,
                if (active) {
                    colors.primary
                } else {
                    colors.outlineVariant
                },
                shape
            )
            .padding(
                horizontal = 12.dp
            ),
        contentAlignment =
            Alignment.Center
    ) {

        Text(
            label,
            style =
                MaterialTheme.typography
                    .labelLarge,
            color =
                if (active) {
                    colors.onPrimary
                } else {
                    colors.onSurface
                }
        )
    }
}
