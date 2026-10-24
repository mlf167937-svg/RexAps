// RexApsApp.kt
package com.rexaps.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal const val THEME_COLUMNS = 3

@Composable
internal fun ThemePicker(
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

@Composable
internal fun ThemeTile(
    option: RexThemeOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val spec = remember(option) { RexPaletteSpec.of(option) }

    val previewBg = Color(spec.background)
    val previewCard = Color(spec.card)
    val previewOutline = Color(spec.outline)
    val previewPrimary = Color(spec.primary)
    val previewTertiary = Color(spec.tertiary)

    val shape = RoundedCornerShape(18.dp)
    val previewShape = RoundedCornerShape(12.dp)

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) colors.primary else colors.outlineVariant,
        animationSpec = tween(250),
        label = "tileBorder"
    )
    val tileScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.96f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tileScale"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "check"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = tileScale
                scaleY = tileScale
            }
            .clip(shape)
            .background(colors.surfaceVariant)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, shape)
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(previewShape)
                .background(Brush.linearGradient(listOf(previewBg, previewCard)))
                .border(1.dp, previewOutline, previewShape)
        ) {
            if (option == RexThemeOption.GLASS_BLUE_MOON) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(5.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = previewPrimary.copy(alpha = .92f)
                ) {
                    Text("Default", modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        color = Color(spec.onPrimary), style = MaterialTheme.typography.labelSmall)
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(16.dp)
                    .background(
                        Brush.linearGradient(listOf(previewPrimary, previewTertiary)),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(18.dp)
                    .graphicsLayer {
                        scaleX = checkScale
                        scaleY = checkScale
                        alpha = checkScale.coerceIn(0f, 1f)
                    }
                    .background(colors.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Dipilih",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Text(
            text = option.label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp)
        )
    }
}
