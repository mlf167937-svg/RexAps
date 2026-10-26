package com.rexaps.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rexaps.ui.theme.RexThemeMaterial

@Composable
internal fun __splitImpl_FeaturedCard(
    app: RexModule,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val visual = currentThemeVisualSpec()
    val glass = visual.material == RexThemeMaterial.GLASS
    val shape = RoundedCornerShape((28 + visual.radiusBias).coerceIn(22, 34).dp)
    val primary = colors.primary
    val outline = colors.outlineVariant
    val glow by animateFloatAsState(
        targetValue = if (isActive) .74f else .25f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "featuredGlow"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pressable(enabled = app.available, onClick = onClick)
            .alpha(if (app.available) 1f else 0.6f)
            .shadow(
                elevation = 16.dp,
                shape = shape,
                ambientColor = primary.copy(alpha = 0.3f),
                spotColor = primary.copy(alpha = 0.3f)
            )
            .clip(shape)
            .background(colors.surfaceVariant.copy(alpha = if (glass) visual.surfaceAlpha else .96f))
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.28f * glow), Color.Transparent),
                        center = Offset(size.width * 0.9f, 0f),
                        radius = size.width * 0.75f
                    ),
                    radius = size.width * 0.75f,
                    center = Offset(size.width * 0.9f, 0f)
                )
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(primary.copy(alpha = if (glass) visual.highlightAlpha else (.3f + 0.5f * glow).coerceIn(0f, 1f)), outline.copy(alpha = visual.borderAlpha))
                ),
                shape = shape
            )
            .padding(24.dp)
    ) {
        RexThemeMotifOverlay(visual, primary, colors.tertiary, Modifier.matchParentSize().alpha(if (glass) .90f else .35f))
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                ModuleArtwork(moduleId = app.id, size = 64.dp)
                StatusPill(available = app.available)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Aplikasi utama",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary
                    )
                }
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (app.available) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accentBrush())
                        .padding(horizontal = 20.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Buka",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
