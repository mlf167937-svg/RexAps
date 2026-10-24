package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

@Composable
internal fun __splitImpl_SyncOverviewCard(
    state: SyncUiState,
    active: Boolean,
    connecting: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = scheme.primaryContainer
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = scheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp).size(28.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Music, in sync.",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "Putar bareng di jaringan Wi-Fi yang sama",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                val statusColor = when {
                    state.error != null -> scheme.errorContainer
                    active -> scheme.tertiaryContainer
                    else -> scheme.surface
                }
                val statusTextColor = when {
                    state.error != null -> scheme.onErrorContainer
                    active -> scheme.onTertiaryContainer
                    else -> scheme.onSurfaceVariant
                }
                Surface(shape = RoundedCornerShape(50), color = statusColor) {
                    Text(
                        when {
                            state.error != null -> "ERROR"
                            state.hosting -> "HOST"
                            state.connected -> "CONNECTED"
                            connecting -> "CONNECTING"
                            else -> "OFFLINE"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusTextColor
                    )
                }
            }

            Text(
                state.status,
                style = MaterialTheme.typography.titleSmall,
                color = if (state.error != null) scheme.error else scheme.onSurface
            )
            if (connecting) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Mencoba menghubungkan…", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
            if (active) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("ROOM", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                        Text(state.room, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("MEMBERS", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                        Text(
                            "${state.members.distinctBy { it.id }.size}",
                            style = MaterialTheme.typography.titleLarge,
                            color = scheme.primary
                        )
                    }
                    if (state.roomElapsedMs > 0L) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("ROOM TIME", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                            Text(formatRoomTime(state.roomElapsedMs), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
            if (state.cacheStatus.isNotBlank()) {
                Text(state.cacheStatus, style = MaterialTheme.typography.bodySmall, color = scheme.tertiary)
            }
            state.error?.let { errorText ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.errorContainer
                ) {
                    Text(
                        errorText,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        color = scheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
internal fun __splitImpl_SyncConnectionCard(
    room: String,
    onRoomChange: (String) -> Unit,
    ip: String,
    onIpChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    active: Boolean,
    onAutoIp: () -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SyncSectionHeading(title = "Connection", subtitle = "Host a room or join a device on your local Wi-Fi")
            OutlinedTextField(
                value = room,
                onValueChange = onRoomChange,
                label = { Text("Nama room") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !active
            )
            OutlinedTextField(
                value = ip,
                onValueChange = onIpChange,
                label = { Text("IP Wi-Fi host") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !active,
                trailingIcon = {
                    TextButton(onClick = onAutoIp, enabled = !active) { Text("AUTO") }
                }
            )
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password room") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !active
            )
            Button(
                onClick = onCreateRoom,
                enabled = !active && ip.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Buat room sebagai host")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Join an existing room", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Nama kamu") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !active
            )
            Button(
                onClick = onJoinRoom,
                enabled = !active && ip.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Gabung room")
            }
            Text(
                "Gunakan Wi-Fi yang sama atau hotspot host. Matikan VPN/WARP jika perangkat tidak bisa saling terhubung.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (active) {
                Text(
                    "Koneksi aktif. Putuskan room terlebih dahulu untuk mengganti detail koneksi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
