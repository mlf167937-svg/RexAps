package com.rexaps.rexmusic.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexmusic.*

@Composable
internal fun RepeatControl(total: Int, left: Int, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.tertiary
    val on = total > 0
    Box(
        Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (on) Icons.Default.RepeatOne else Icons.Default.Repeat,
            contentDescription = if (on) "Ulangi lagu, sisa $left dari $total kali" else "Atur ulangi lagu",
            tint = if (on) accent else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.size(26.dp)
        )
        if (on) {
            Text(
                "$left", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp)
                    .background(accent, CircleShape)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                    .size(4.dp).background(accent, CircleShape)
            )
        }
    }
}

@Composable
internal fun ActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.tertiary
    val fg = if (active) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onBackground
    Row(
        Modifier.height(40.dp).clip(CircleShape)
            .background(if (active) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

//  Error & neighbors 

@Composable
internal fun PlayerError(error: String?, onRetry: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AnimatedVisibility(visible = !error.isNullOrBlank()) {
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            shape = RoundedCornerShape(12.dp), color = scheme.errorContainer
        ) {
            Row(
                Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Error", tint = scheme.onErrorContainer)
                Spacer(Modifier.width(10.dp))
                Text(
                    error.orEmpty(), modifier = Modifier.weight(1f),
                    color = scheme.onErrorContainer, style = MaterialTheme.typography.bodySmall
                )
                TextButton(onClick = onRetry) { Text("Coba lagi") }
            }
        }
    }
}

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
