package com.rexaps.rexpanel

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun RexPanelScreenConnectHeader(
    onExit: () -> Unit,
    accent: Brush,
    onPreset: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
        IconButton(
            onClick = onExit
        ) {
            Icon(
                Icons.Default.ArrowBack,
                "Kembali"
            )
        }

        Spacer(
            Modifier.height(16.dp)
        )

        Box(
            Modifier
                .size(64.dp)
                .clip(
                    RoundedCornerShape(20.dp)
                )
                .background(accent),
            contentAlignment =
                Alignment.Center
        ) {
            Text(
                ">_",
                color = colors.onPrimary,
                fontWeight =
                    FontWeight.Bold,
                fontFamily =
                    FontFamily.Monospace,
                fontSize = 22.sp
            )
        }

        Spacer(
            Modifier.height(20.dp)
        )

        Text(
            "RexPanel",
            style =
                MaterialTheme.typography
                    .headlineLarge
        )

        Text(
            "Monitor, terminal, dan file server lewat SSH. Tidak perlu root.",
            style =
                MaterialTheme.typography
                    .bodyMedium,
            color =
                colors.onSurfaceVariant
        )

        Spacer(
            Modifier.height(24.dp)
        )

        Row(
            Modifier.horizontalScroll(
                rememberScrollState()
            ),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            PresetChip(
                "Termux · 8022"
            ) {
                onPreset(
                    "ssh -p 8022 user@192.168."
                )
            }

            PresetChip(
                "Server Linux · 22"
            ) {
                onPreset(
                    "ssh user@"
                )
            }
        }

        Spacer(
            Modifier.height(12.dp)
        )
}
