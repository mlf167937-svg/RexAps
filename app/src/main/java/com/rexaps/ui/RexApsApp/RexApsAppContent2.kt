package com.rexaps.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.ui.theme.RexThemeMaterial
import com.rexaps.ui.theme.RexThemeVisuals

@Composable
internal fun __splitImpl_ThemeTile(
    option: RexThemeOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val spec = remember(option) { RexPaletteSpec.of(option) }
    val visual = remember(option) { RexThemeVisuals.forTheme(option.labelToThemeId()) }

    val previewBg = Color(spec.background)
    val previewCard = Color(spec.card)
    val previewOutline = Color(spec.outline).copy(alpha = visual.borderAlpha)
    val previewPrimary = Color(spec.primary)
    val previewTertiary = Color(spec.tertiary)
    val glass = visual.material == RexThemeMaterial.GLASS

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
                .height(72.dp)
                .clip(previewShape)
                .background(previewBg)
                .border(1.dp, previewOutline, previewShape)
        ) {
            Box(
                Modifier.matchParentSize().background(
                    Brush.linearGradient(
                        listOf(
                            if (glass) previewCard.copy(alpha = visual.surfaceAlpha) else previewCard.copy(alpha = .88f),
                            previewBg.copy(alpha = .84f),
                            previewPrimary.copy(alpha = if (glass) .12f else .22f)
                        )
                    )
                )
            )
            RexThemeMotifOverlay(
                visual, previewPrimary, previewTertiary,
                Modifier.matchParentSize().alpha(if (glass) .95f else .78f)
            )
            if (glass) {
                Box(
                    Modifier.align(Alignment.TopStart).fillMaxWidth().height(17.dp).background(
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = visual.highlightAlpha), Color.Transparent))
                    )
                )
            }
            // Material sample makes the tile preview communicate the theme's surface treatment,
            // especially the genuinely translucent Glass variants.
            val sampleShape = RoundedCornerShape(8.dp)
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 22.dp)
                    .size(width = 56.dp, height = 34.dp)
                    .clip(sampleShape)
                    .background(previewCard.copy(alpha = if (glass) visual.surfaceAlpha else .92f))
                    .border(1.dp, previewPrimary.copy(alpha = if (glass) visual.borderAlpha else .36f), sampleShape)
            ) {
                Box(
                    Modifier.fillMaxWidth().height(6.dp).background(
                        Brush.horizontalGradient(
                            listOf(Color.White.copy(alpha = visual.highlightAlpha), Color.Transparent)
                        )
                    )
                )
                Column(Modifier.align(Alignment.Center).padding(horizontal = 7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(Modifier.fillMaxWidth(.72f).height(2.dp).clip(CircleShape).background(previewPrimary.copy(alpha = .85f)))
                    Box(Modifier.fillMaxWidth(.46f).height(2.dp).clip(CircleShape).background(previewTertiary.copy(alpha = .82f)))
                }
            }

            if (option == RexThemeOption.DARK_BLUE_MOON) {
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
                    .border(1.dp, Color.White.copy(alpha = visual.highlightAlpha), CircleShape)
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
                    .background(colors.primary, CircleShape)
                    .border(1.dp, Color.White.copy(alpha = .45f), CircleShape),
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

@Composable
internal fun __splitImpl_SocialLinkCard(link: SocialLink, modifier: Modifier = Modifier) {

    val colors = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "pressScale"
    )

    Surface(
        onClick = {
            try {
                uriHandler.openUri(link.url)
            } catch (_: Exception) {
                // Tidak ada aplikasi/browser untuk membuka link.
            }
        },
        interactionSource = interaction,
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(link.color.copy(alpha = 0.55f), link.color.copy(alpha = 0.08f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        ambientColor = link.color.copy(alpha = 0.5f),
                        spotColor = link.color.copy(alpha = 0.5f)
                    )
                    .background(
                        Brush.linearGradient(
                            listOf(link.color, link.color.copy(alpha = 0.7f))
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = link.symbol,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.size(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = link.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = link.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(link.color.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "↗",
                    color = link.color.copy(alpha = 0.95f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
