package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import com.rexaps.ui.RexVideo
import kotlinx.coroutines.*

@Composable
internal fun __splitImpl_SyncMemberCard(
    member: SyncMember,
    state: SyncUiState,
    onVolumeChange: (Float) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val bannerBitmap = remember(member.bannerBase64) { decodeRexSyncProfileImage(member.bannerBase64) }
    val avatarBitmap = remember(member.profilePictureBase64) { decodeRexSyncProfileImage(member.profilePictureBase64) }
    val bannerVideoPath by produceState<String?>(null, member.id, member.bannerVideoBase64) {
        value = withContext(Dispatchers.IO) { cacheRexSyncProfileVideo(context, member.bannerVideoBase64, member.id, "banner") }
    }
    val avatarVideoPath by produceState<String?>(null, member.id, member.profilePictureVideoBase64) {
        value = withContext(Dispatchers.IO) { cacheRexSyncProfileVideo(context, member.profilePictureVideoBase64, member.id, "avatar") }
    }
    val isHost = member.id == "host"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                    .background(Brush.linearGradient(listOf(scheme.primary.copy(alpha = 0.62f), scheme.tertiary.copy(alpha = 0.36f), scheme.surfaceVariant)))
            ) {
                val liveBanner = bannerVideoPath?.takeIf { java.io.File(it).isFile }
                if (liveBanner != null) {
                    RexVideo(
                        path = liveBanner,
                        modifier = Modifier.fillMaxSize(),
                        key = "${member.id}-banner-${member.bannerVideoBase64.hashCode()}",
                        loop = true,
                        muted = true,
                        playing = true,
                        fit = false
                    )
                } else if (bannerBitmap != null) {
                    coil.compose.AsyncImage(
                        model = bannerBitmap,
                        contentDescription = "Banner ${member.name}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.42f)))))
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(11.dp),
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.48f)
                ) {
                    Text(
                        if (isHost) "HOST" else "CONNECTED",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(58.dp).clip(CircleShape)
                        .background(scheme.secondaryContainer)
                        .border(2.dp, scheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val liveAvatar = avatarVideoPath?.takeIf { java.io.File(it).isFile }
                    if (liveAvatar != null) {
                        RexVideo(
                            path = liveAvatar,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            key = "${member.id}-avatar-${member.profilePictureVideoBase64.hashCode()}",
                            loop = true,
                            muted = true,
                            playing = true,
                            fit = false
                        )
                    } else if (avatarBitmap != null) {
                        coil.compose.AsyncImage(
                            model = avatarBitmap,
                            contentDescription = "Foto profil ${member.name}",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        androidx.compose.material3.Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(29.dp),
                            tint = scheme.onSecondaryContainer
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(member.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(
                        member.username.ifBlank { if (isHost) "Host device" else "${member.ip}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.primary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (member.bio.isNotBlank()) {
                        Text(member.bio, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
                if (isHost) {
                    Surface(shape = RoundedCornerShape(10.dp), color = scheme.primaryContainer) {
                        Text("YOU", modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = scheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (state.hosting && !isHost) {
                Divider(modifier = Modifier.padding(horizontal = 16.dp), color = scheme.outlineVariant.copy(alpha = 0.45f))
                Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Volume perangkat Android", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        Text(
                            "${((state.volumes[member.id] ?: member.volume).coerceIn(0f, 1f) * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.primary
                        )
                    }
                    Slider(
                        value = (state.volumes[member.id] ?: member.deviceVolume.takeIf { it >= 0f } ?: member.volume).coerceIn(0f, 1f),
                        onValueChange = onVolumeChange,
                        valueRange = 0f..1f
                    )
                    Text(
                        if (member.deviceVolume >= 0f) "Volume media aktual: ${(member.deviceVolume * 100).toInt()}% · diubah langsung pada perangkat anggota"
                        else "Menunggu informasi volume media perangkat…",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            } else if (!isHost && member.deviceVolume >= 0f) {
                Text(
                    "Volume media Android: ${(member.deviceVolume * 100).toInt()}%",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 13.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun __splitImpl_SyncWaitingCard(connecting: Boolean, error: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (error != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (connecting) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
            } else {
                androidx.compose.material3.Icon(
                    Icons.Default.Groups,
                    contentDescription = null,
                    tint = if (error != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                if (connecting) "Connecting to room…" else if (error != null) "Connection needs attention" else "Not connected to a room",
                style = MaterialTheme.typography.titleMedium,
                color = if (error != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                if (connecting) "RexMusic is trying to establish the local Wi-Fi connection." else "Create a room on this device or enter a host IP to join an existing room.",
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun __splitImpl_SyncOnlineSearchCard(
    query: String,
    onQueryChange: (String) -> Unit,
    searching: Boolean,
    searchMessage: String,
    onSearch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SyncSectionHeading("Find music", "Search online tracks to download for the shared room")
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Judul lagu atau artis") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !searching,
                trailingIcon = {
                    TextButton(
                        enabled = !searching && query.isNotBlank(),
                        onClick = onSearch
                    ) {
                        Text(if (searching) "…" else "CARI")
                    }
                }
            )
            if (searching) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Mencari lagu…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (searchMessage.isNotBlank()) {
                Text(
                    searchMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun __splitImpl_SyncOnlineResultCard(
    track: RexTrack,
    downloaded: Boolean,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            TextButton(onClick = onDownload) {
                Text(if (downloaded) "TERSIMPAN" else "UNDUH")
            }
        }
    }
}

@Composable
internal fun __splitImpl_SyncSectionHeading(
    title: String,
    subtitle: String? = null,
    titleColor: androidx.compose.ui.graphics.Color? = null,
    subtitleColor: androidx.compose.ui.graphics.Color? = null
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor ?: scheme.primary)
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor ?: scheme.onSurfaceVariant
            )
        }
    }
}
