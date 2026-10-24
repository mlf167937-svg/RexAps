package com.rexaps.rexpanel

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun PanelHeader(
    target: SshTarget,
    host: String?,
    onMinimize: () -> Unit,
    onDisconnect: () -> Unit
) {
    val colors =
        MaterialTheme.colorScheme

    var confirm by remember {
        mutableStateOf(false)
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp,
                vertical = 6.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        IconButton(
            onClick = onMinimize
        ) {
            Icon(
                Icons.Default.ArrowBack,
                "Kembali"
            )
        }

        Column(
            Modifier.weight(1f)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    Modifier
                        .size(8.dp)
                        .background(
                            Good,
                            CircleShape
                        )
                )

                Spacer(
                    Modifier.width(8.dp)
                )

                Text(
                    host ?: target.host,
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Spacer(
                    Modifier.width(8.dp)
                )

                Text(
                    "Terhubung",
                    style =
                        MaterialTheme.typography
                            .labelSmall,
                    color =
                        colors.onSurfaceVariant
                )
            }

            Text(
                "${target.user}@${target.host}:${target.port}",
                style =
                    MaterialTheme.typography
                        .bodySmall,
                color =
                    colors.onSurfaceVariant,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = {
                confirm = true
            }
        ) {
            Icon(
                Icons.Default.PowerSettingsNew,
                "Putuskan koneksi",
                tint = colors.error
            )
        }
    }

    if (confirm) {

        AlertDialog(
            onDismissRequest = {
                confirm = false
            },

            title = {
                Text(
                    "Putuskan koneksi?"
                )
            },

            text = {
                Text(
                    "Sesi terminal akan ditutup dan perintah yang sedang berjalan bisa berhenti."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        confirm = false
                        onDisconnect()
                    }
                ) {
                    Text(
                        "Putuskan",
                        color = colors.error
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        confirm = false
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}
