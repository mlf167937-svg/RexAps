package com.rexaps.rexmusic

import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.ServerSocket

@Composable
internal fun SyncConnectionCard(
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

fun RexMusicSyncEngine.__split_detectIp_1(context: Context): String = runCatching {
        // Works for Wi-Fi client and many Android hotspot interfaces; avoid VPN/tunnel interfaces.
        val interfaces = java.net.NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        val candidates = interfaces.filter { ni ->
            ni.isUp && !ni.isLoopback && !ni.name.lowercase().matches(Regex(".*(tun|tap|wg|warp|vpn|dummy).*"))
        }.flatMap { ni -> ni.inetAddresses.toList().filterIsInstance<java.net.Inet4Address>()
            .filter { !it.isLoopbackAddress && !it.isLinkLocalAddress }
            .map { ni.name.lowercase() to it.hostAddress.orEmpty() } }
        candidates.firstOrNull { (name, ip) -> name.matches(Regex(".*(wlan|wifi|ap|swlan|p2p|eth).*")) && ip.isNotBlank() }?.second
            ?: candidates.firstOrNull { it.second.isNotBlank() }?.second.orEmpty()
    }.getOrDefault("")

fun RexMusicSyncEngine.__split_createRoom_2(room: String, password: String, ip: String) {
        closeAll()
        roomName = room.trim().ifBlank { "RexSyncRoom1" }
        roomStartedElapsedMs = SystemClock.elapsedRealtime()
        roomPassword = password
        _state.value = _state.value.copy(hosting = true, connected = true, room = roomName,
            password = password, ip = ip, status = "Membuka room…", error = null,
            members = listOf(SyncMember("host", "Host")))
        roomJob = scope.launch {
            try {
                val ss = ServerSocket(SYNC_PORT, 32, InetAddress.getByName("0.0.0.0"))
                server = ss
                _state.value = _state.value.copy(status = "Room aktif · menunggu anggota")
                launch { monitorPeers() }
                launch { while (isActive && _state.value.hosting) { broadcastTick(); delay(900L) } }
                while (!ss.isClosed) {
                    val socket = ss.accept().apply { soTimeout = 0; tcpNoDelay = true; keepAlive = true }
                    launch { acceptPeer(socket) }
                }
            } catch (e: Exception) {
                if (_state.value.hosting) _state.value = _state.value.copy(status = "Server gagal: ${e.message}", error = e.message)
            }
        }
    }
