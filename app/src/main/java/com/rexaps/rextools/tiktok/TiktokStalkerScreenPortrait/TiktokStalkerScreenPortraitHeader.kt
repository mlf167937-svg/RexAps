package com.rexaps.rextools.tiktok

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rexaps.rextools.ui.*

@Composable
internal fun __splitImpl_ProfileHeader(
    avatar: Any?, name: String?, username: String?, bio: String?,
    verified: Boolean, region: String?
) {
    val s = MaterialTheme.colorScheme
    val badge by rememberInfiniteTransition(label = "badge")
        .animateFloat(1f, 1.18f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")
    val drift by rememberInfiniteTransition(label = "banner")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "d")

    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth().height(104.dp).background(
                Brush.linearGradient(
                    listOf(
                        s.primary.copy(alpha = 0.30f + 0.12f * drift),
                        s.tertiary.copy(alpha = 0.28f),
                        s.secondary.copy(alpha = 0.18f + 0.10f * (1 - drift))
                    )
                )
            )
        )
        Column(
            Modifier.fillMaxWidth().padding(top = 52.dp).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RexAvatar(
                model = avatar,
                contentDescription = name?.let { "Foto profil $it" } ?: "Foto profil",
                size = 108.dp,
                modifier = Modifier.entrance(0)
            )
            Column(
                Modifier.entrance(1),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        name ?: "-",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f, fill = false).semantics { heading() }
                    )
                    if (verified) {
                        Icon(
                            Icons.Default.CheckCircle, contentDescription = "Verified", tint = s.primary,
                            modifier = Modifier.size(20.dp).graphicsLayer { scaleX = badge; scaleY = badge }
                        )
                    }
                }
                Text("@${username ?: "-"}", style = MaterialTheme.typography.bodyMedium, color = s.onSurfaceVariant)
            }
            bio?.takeIf { it.isNotBlank() && it != "-" }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.entrance(2))
            }
            region?.takeIf { it.isNotBlank() && it != "-" }?.let {
                RexPill("Region: $it", Modifier.entrance(3), icon = Icons.Default.Public)
            }
        }
    }
}
