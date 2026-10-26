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
internal fun __splitImpl_AppCard(
    app: RexModule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val visual = currentThemeVisualSpec()
    val glass = visual.material == RexThemeMaterial.GLASS
    val shape = RoundedCornerShape((24 + visual.radiusBias).coerceIn(18, 30).dp)
    val borderColor = if (app.available) colors.primary.copy(alpha = if (glass) visual.borderAlpha else .35f) else colors.outlineVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(166.dp)
            .pressable(enabled = app.available, onClick = onClick)
            .alpha(if (app.available) 1f else 0.55f)
            .shadow(
                elevation = if (app.available) 5.dp else 0.dp,
                shape = shape,
                ambientColor = colors.primary.copy(alpha = if (glass) .16f else .20f),
                spotColor = colors.primary.copy(alpha = if (glass) .14f else .20f)
            )
            .clip(shape)
            .background(colors.surfaceVariant.copy(alpha = if (glass) visual.surfaceAlpha else .96f))
            .border(1.dp, borderColor, shape)
    ) {
        if (!glass) {
            Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(colors.primary.copy(alpha = .055f), Color.Transparent))))
        }
        RexThemeMotifOverlay(visual, colors.primary, colors.tertiary, Modifier.matchParentSize().alpha(if (glass) .86f else .26f))
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                ModuleArtwork(moduleId = app.id, size = 48.dp)

                if (app.available) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(colors.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.outlineVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Segera",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun __splitImpl_HeroCard(
    appCount: Int,
    availableCount: Int,
    themeLabel: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val visual = currentThemeVisualSpec()
    val glass = visual.material == RexThemeMaterial.GLASS
    val onAccent = if (glass) colors.onSurface else colors.onPrimary

    val drift by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(460, easing = FastOutSlowInEasing),
        label = "heroDrift"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape((28 + visual.radiusBias).coerceIn(22, 34).dp))
            .background(if (glass) Brush.linearGradient(listOf(colors.surface.copy(alpha = visual.surfaceAlpha), colors.primary.copy(alpha = visual.surfaceAlpha * .62f))) else accentBrush())
            .drawBehind {
                drawCircle(
                    color = onAccent.copy(alpha = 0.14f),
                    radius = 120.dp.toPx(),
                    center = Offset(
                        x = size.width - 30.dp.toPx() - drift * 30.dp.toPx(),
                        y = 10.dp.toPx() + drift * 24.dp.toPx()
                    )
                )
                drawCircle(
                    color = onAccent.copy(alpha = 0.10f),
                    radius = 70.dp.toPx(),
                    center = Offset(
                        x = size.width - 90.dp.toPx() + drift * 36.dp.toPx(),
                        y = size.height - 10.dp.toPx() - drift * 20.dp.toPx()
                    )
                )
            }
    ) {
        RexThemeMotifOverlay(visual, colors.primary, colors.tertiary, Modifier.matchParentSize().alpha(if (glass) .95f else .24f))
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(onAccent.copy(alpha = 0.18f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$appCount aplikasi",
                    style = MaterialTheme.typography.labelMedium,
                    color = onAccent
                )
            }

            Text(
                text = "Semua aplikasimu,\nsatu tempat.",
                style = MaterialTheme.typography.headlineSmall,
                color = onAccent
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroStatChip(label = "Tersedia", value = "$availableCount", onAccent = onAccent)
                HeroStatChip(label = "Tema aktif", value = themeLabel, onAccent = onAccent)
            }
        }
    }
}

@Composable
internal fun __splitImpl_ThemePicker(
    selected: RexThemeOption,
    onSelect: (RexThemeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryGroups = listOf(
        "Dark" to RexThemeOption.curatedOptions.filter { it.name.startsWith("DARK_") },
        "Glass" to RexThemeOption.curatedOptions.filter { it.name.startsWith("GLASS_") },
        "Light" to RexThemeOption.curatedOptions.filter { it.name.startsWith("LIGHT_") },
        "Neon" to RexThemeOption.curatedOptions.filter { it.name.startsWith("NEON_") },
        "Cosmic" to RexThemeOption.curatedOptions.filter { it.name.startsWith("ASTRAL_") || it.name.startsWith("NEBULA_") || it.name.startsWith("ECLIPSE_") || it.name.startsWith("STARLIGHT_") },
        "Elemental" to RexThemeOption.curatedOptions.filter { it.name.startsWith("FROST_") || it.name.startsWith("INFERNO_") || it.name.startsWith("THUNDER_") || it.name.startsWith("ABYSSAL_") }
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        categoryGroups.forEach { (category, options) ->
            Text(category, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            options.chunked(THEME_COLUMNS).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { option ->
                        ThemeTile(option = option, isSelected = option == selected,
                            onClick = { onSelect(option) }, modifier = Modifier.weight(1f))
                    }
                    repeat(THEME_COLUMNS - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
