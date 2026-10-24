package com.rexaps.rexmusic.player

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rexaps.rexmusic.*

@Composable
internal fun AutoplayRow(favorites: List<String>, popularCount: Int, offlineMode: Boolean, onClick: () -> Unit) {
    val subtitle = when {
        offlineMode -> "Dipilih dari lagu unduhanmu"
        favorites.isNotEmpty() -> "Dicampur dari ${favorites.joinToString(", ")} & lagu populer"
        popularCount > 0 -> "Dipilih acak dari lagu populer"
        else -> "Dipilih otomatis untukmu"
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
            .clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(RexHeroBrush),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("AUTOPLAY PINTAR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            Text("Lagu berikutnya untukmu", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
            Text(
                subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.SkipNext, contentDescription = "Lagu berikutnya", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
    }
}
