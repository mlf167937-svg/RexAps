package com.rexaps.rexpanel

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp

internal enum class PanelTab(
    val label: String,
    val icon: ImageVector
) {
    Dashboard("Dasbor", Icons.Default.Dashboard),
    Terminal("Terminal", Icons.Default.Terminal),
    Files("File", Icons.Default.Folder)
}

@Composable
internal fun PanelTabs(
    selected: PanelTab,
    onSelect: (PanelTab) -> Unit
) {
    val colors =
        MaterialTheme.colorScheme

    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 6.dp
            )
            .clip(
                RoundedCornerShape(18.dp)
            )
            .background(
                colors.surfaceVariant
            )
            .padding(4.dp)
            .selectableGroup(),

        horizontalArrangement =
            Arrangement.spacedBy(4.dp)
    ) {

        PanelTab.values().forEach { t ->

            val sel =
                t == selected

            val fg =
                if (sel) {
                    colors.onPrimary
                } else {
                    colors.onSurfaceVariant
                }

            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(
                        RoundedCornerShape(14.dp)
                    )
                    .background(
                        if (sel) {
                            colors.primary
                        } else {
                            Color.Transparent
                        }
                    )
                    .selectable(
                        selected = sel,
                        role = Role.Tab,
                        onClick = {
                            onSelect(t)
                        }
                    )
                    .padding(
                        horizontal = 8.dp
                    ),

                horizontalArrangement =
                    Arrangement.Center,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    t.icon,
                    null,
                    Modifier.size(18.dp),
                    tint = fg
                )

                Spacer(
                    Modifier.width(6.dp)
                )

                Text(
                    t.label,
                    style =
                        MaterialTheme.typography
                            .labelLarge,
                    color = fg,
                    maxLines = 1
                )
            }
        }
    }
}

/* -------------------------------- MONITOR -------------------------------- */
