// RexAppsScreenPortrait.kt
package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val proGradients = listOf(
    listOf(Color(0xFF7C3AED), Color(0xFFEC4899)),
    listOf(Color(0xFF0EA5E9), Color(0xFF6366F1)),
    listOf(Color(0xFF10B981), Color(0xFF06B6D4)),
    listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
    listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6)),
    listOf(Color(0xFFF43F5E), Color(0xFFF97316))
)

internal fun proGradientFor(key: String): List<Color> =
    proGradients[key.hashCode().mod(proGradients.size)]

/** Permukaan kaca: gradient halus dengan border tipis berwarna tema. */
@Composable
internal fun ProGlass(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val c = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(c.surfaceVariant.copy(alpha = 0.82f), c.surface.copy(alpha = 0.6f))
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(c.primary.copy(alpha = 0.30f), c.outlineVariant.copy(alpha = 0.20f))
                ),
                shape
            ),
        content = content
    )
}

@Composable
internal fun ProIcon(name: String, colors: List<Color>, dim: Dp) {
    val shape = RoundedCornerShape(dim * 0.34f)
    Box(
        Modifier
            .size(dim)
            .shadow(elevation = 12.dp, shape = shape, spotColor = colors.first().copy(alpha = 0.5f))
            .clip(shape)
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1).uppercase(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
internal fun ProAppTile(
    app: RexModule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProGlass(
        modifier
            .fillMaxWidth()
            .height(180.dp)
            .alpha(if (app.available) 1f else 0.55f)
            .proPress(enabled = app.available, onClick = onClick),
        radius = 26.dp
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProIcon(app.name, proGradientFor(app.name), 50.dp)
                ProStatusDot(app.available)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
