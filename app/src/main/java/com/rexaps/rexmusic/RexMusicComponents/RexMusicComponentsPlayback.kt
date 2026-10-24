package com.rexaps.rexmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun AutoplayCard(state: RexMusicUiState, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val subtitle = when {
        !state.autoplay -> "Mati: lagu berikutnya mengikuti urutan daftar"
        state.offline.enabled -> "Dipilih dari lagu unduhanmu"
        state.favoriteArtists.isNotEmpty() ->
            "Dicampur dari ${state.favoriteArtists.joinToString(", ")} & lagu populer"
        state.popularCount > 0 -> "Dipilih acak dari lagu populer"
        else -> "Dipilih otomatis untukmu"
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.primary.copy(alpha = if (state.autoplay) 0.14f else 0.06f))
            .clickable(role = Role.Switch, onClick = onToggle)
            .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = scheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Autoplay pintar", style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle, style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = state.autoplay, onCheckedChange = { onToggle() })
    }
}
