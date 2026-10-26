
package com.rexaps.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.RexThemeMaterial
import com.rexaps.ui.theme.RexThemeVisuals

@Composable
fun RexButton(
    label: String,
    tokens: RexDesignTokens,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val hovered by source.collectIsHoveredAsState()
    val focused by source.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> .955f
            hovered -> 1.018f
            focused -> 1.008f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = .64f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "rex-button-scale"
    )

    val elevation by animateDpAsState(
        targetValue = when {
            !enabled -> 0.dp
            pressed -> tokens.shadowSm.dp
            hovered || focused -> tokens.shadowLg.dp
            else -> tokens.shadowMd.dp
        },
        animationSpec = tween(
            durationMillis = tokens.motionFast
        ),
        label = "rex-button-elevation"
    )

    val targetColor = when {
        !enabled -> tokens.textDisabled
        pressed -> tokens.primaryPressed
        hovered || focused -> tokens.primaryHover
        else -> tokens.primary
    }

    val container by animateColorAsState(
        targetValue = Color(targetColor),
        animationSpec = tween(
            durationMillis = tokens.motionFast
        ),
        label = "rex-button-color"
    )

    val shape = RoundedCornerShape(tokens.radiusMd.dp)

    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        interactionSource = source,
        modifier = modifier
            .heightIn(min = 44.dp)
            .shadow(elevation, shape)
            .scale(scale)
            .animateContentSize(),
        shape = shape,
        border = if (focused) {
            BorderStroke(
                tokens.borderThick.dp,
                Color(tokens.accent)
            )
        } else {
            null
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = Color(tokens.onPrimary),
            disabledContainerColor = Color(tokens.textDisabled),
            disabledContentColor = Color(tokens.surface)
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(17.dp),
                strokeWidth = 2.dp,
                color = Color(tokens.onPrimary)
            )
        } else {
            Text(label)
        }
    }
}

@Composable
fun RexCard(
    tokens: RexDesignTokens,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val visual = RexThemeVisuals.forTheme(tokens.id)
    val glass = visual.material == RexThemeMaterial.GLASS
    val shape = RoundedCornerShape((tokens.radiusLg + visual.radiusBias).coerceIn(16, 36).dp)
    val baseSurface = Color(tokens.surface)

    Card(
        modifier = modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = .86f,
                stiffness = Spring.StiffnessLow
            )
        ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = baseSurface.copy(alpha = if (glass) visual.surfaceAlpha else 1f)
        ),
        border = BorderStroke(
            (if (glass) tokens.borderThin else tokens.borderHairline).dp,
            Color(tokens.border).copy(alpha = if (glass) visual.borderAlpha else .82f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = (if (glass) tokens.shadowSm else tokens.shadowMd).dp,
            pressedElevation = tokens.shadowSm.dp,
            focusedElevation = (if (glass) tokens.shadowMd else tokens.shadowLg).dp,
            hoveredElevation = (if (glass) tokens.shadowMd else tokens.shadowLg).dp
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(tokens.accent).copy(alpha = if (glass) visual.highlightAlpha * .28f else .035f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun RexSkeleton(
    tokens: RexDesignTokens,
    modifier: Modifier = Modifier,
    height: Int = 16,
    animate: Boolean = true
) {
    val transition = rememberInfiniteTransition(
        label = "rex-skeleton-shimmer"
    )

    val sweep by transition.animateFloat(
        initialValue = if (animate) -320f else 0f,
        targetValue = if (animate) 520f else 0f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween<Float>(
                durationMillis =
                    (tokens.motionSlower * 2).coerceAtLeast(1),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rex-skeleton-sweep"
    )

    val shape = RoundedCornerShape(tokens.radiusSm.dp)

    val shimmer = if (animate) {
        Brush.linearGradient(
            colors = listOf(
                Color(tokens.surfaceHover),
                Color(tokens.accent).copy(alpha = .66f),
                Color(tokens.surfaceElevated)
            ),
            start = Offset(sweep, 0f),
            end = Offset(sweep + 320f, height * 3f)
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(tokens.surfaceElevated),
                Color(tokens.surfaceHover)
            )
        )
    }

    Spacer(
        modifier
            .height(height.dp)
            .background(shimmer, shape)
    )
}
