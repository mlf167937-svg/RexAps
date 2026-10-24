package com.rexaps.rexmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun OfflineHeroCard(
    count: Int,
    bytes: Long,
    enabled: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(RexOfflineBrush).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.OfflinePin, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(16.dp)
                )
                Text(
                    "MODE OFFLINE", style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f), letterSpacing = 1.5.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (count == 0) "Belum ada unduhan" else "$count lagu tersimpan",
                style = MaterialTheme.typography.headlineSmall, color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${formatBytes(bytes)}  ${RexOfflineManager.DISPLAY_PATH}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.88f),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroButton("Putar semua", Icons.Default.PlayArrow, true, enabled, onPlayAll, Modifier.weight(1f))
            HeroButton("Acak", Icons.Default.Shuffle, false, enabled, onShuffle, Modifier.weight(1f))
        }
    }
}

@Composable
internal fun HeroButton(
    label: String,
    icon: ImageVector,
    filled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (filled) Color.White else Color.White.copy(alpha = 0.2f)
    val fg = if (filled) RexPlayerBase else Color.White
    Row(
        modifier.height(44.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .clip(CircleShape).background(bg)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
internal fun PermissionCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(scheme.errorContainer.copy(alpha = 0.7f))
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Folder, contentDescription = null, tint = scheme.onErrorContainer)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Izin penyimpanan dibutuhkan", style = MaterialTheme.typography.titleSmall,
                color = scheme.onErrorContainer
            )
            Text(
                "Ketuk untuk mengizinkan akses ke ${RexOfflineManager.DISPLAY_PATH}",
                style = MaterialTheme.typography.bodySmall, color = scheme.onErrorContainer
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = scheme.onErrorContainer)
    }
}

@Composable
internal fun LibraryChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Box(
        Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(
                if (selected) scheme.primary
                else scheme.surfaceVariant
            )
            .clickable(
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 0) "$label $count" else label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) scheme.onPrimary
            else scheme.onSurface,
            maxLines = 1
        )
    }
}
