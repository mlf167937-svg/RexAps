package com.rexaps.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily
import com.rexaps.ui.theme.RexDesignTokens

private fun rexComponentFont(token: String): FontFamily = when (token.lowercase()) {
    "serif" -> FontFamily.Serif
    "monospace" -> FontFamily.Monospace
    "cursive" -> FontFamily.Cursive
    else -> FontFamily.SansSerif
}

@Composable
fun RexButton(
    label: String, tokens: RexDesignTokens, modifier: Modifier = Modifier,
    enabled: Boolean = true, loading: Boolean = false, onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val hovered by source.collectIsHoveredAsState()
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = when { pressed -> .955f; hovered -> 1.018f; focused -> 1.008f; else -> 1f },
        animationSpec = spring(dampingRatio = .64f, stiffness = Spring.StiffnessMediumLow),
        label = "rex-button-scale"
    )
    val elevation by animateDpAsState(
        targetValue = when { !enabled -> 0.dp; pressed -> tokens.shadowSm.dp; hovered || focused -> tokens.shadowLg.dp; else -> tokens.shadowMd.dp },
        animationSpec = tween(tokens.motionFast, easing = FastOutSlowInEasing),
        label = "rex-button-elevation"
    )
    val targetColor = when { !enabled -> tokens.textDisabled; pressed -> tokens.primaryPressed; hovered || focused -> tokens.primaryHover; else -> tokens.primary }
    val container by animateColorAsState(Color(targetColor), tween(tokens.motionFast), label = "rex-button-color")
    val shape = RoundedCornerShape(tokens.radiusMd.dp)
    Button(
        onClick = onClick, enabled = enabled && !loading, interactionSource = source,
        modifier = modifier.heightIn(min = 44.dp).shadow(elevation, shape).scale(scale).animateContentSize(),
        shape = shape,
        border = if (focused) BorderStroke(tokens.borderThick.dp, Color(tokens.accent)) else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = Color(tokens.onPrimary),
            disabledContainerColor = Color(tokens.textDisabled),
            disabledContentColor = Color(tokens.surface)
        )
    ) {
        if (loading) CircularProgressIndicator(
            Modifier.size(17.dp), strokeWidth = 2.dp, color = Color(tokens.onPrimary)
        ) else Text(label)
    }
}

@Composable
fun RexCard(tokens: RexDesignTokens, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(tokens.radiusLg.dp)
    Card(
        modifier = modifier.animateContentSize(animationSpec = spring(dampingRatio = .86f, stiffness = Spring.StiffnessLow)),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color(tokens.surface)),
        border = BorderStroke(tokens.borderThin.dp, Color(tokens.border).copy(alpha = .82f)),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.shadowMd.dp, pressedElevation = tokens.shadowSm.dp, focusedElevation = tokens.shadowLg.dp, hoveredElevation = tokens.shadowLg.dp)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun RexSkeleton(tokens: RexDesignTokens, modifier: Modifier = Modifier, height: Int = 16, animate: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "rex-skeleton-shimmer")
    val sweep by transition.animateFloat(
        initialValue = if (animate) -320f else 0f, targetValue = if (animate) 520f else 0f,
        animationSpec = if (animate) infiniteRepeatable(
            animation = tween(durationMillis = tokens.motionSlower * 2, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ) else tween(0),
        label = "rex-skeleton-sweep"
    )
    val shape = RoundedCornerShape(tokens.radiusSm.dp)
    val shimmer = if (animate) Brush.linearGradient(
        colors = listOf(Color(tokens.surfaceHover), Color(tokens.accent).copy(alpha = .66f), Color(tokens.surfaceElevated)),
        start = Offset(sweep, 0f), end = Offset(sweep + 320f, height * 3f)
    ) else Brush.linearGradient(listOf(Color(tokens.surfaceElevated), Color(tokens.surfaceHover)))
    Spacer(modifier.height(height.dp).background(shimmer, shape))
}
