package com.rexaps.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rexaps.ui.theme.RexDesignTokens

@Composable
fun RexButton(
    label: String, tokens: RexDesignTokens, modifier: Modifier = Modifier,
    enabled: Boolean = true, loading: Boolean = false, onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed) .97f else 1f, label = "rex-button-press")
    val container by animateColorAsState(Color(if(enabled) tokens.primary else tokens.textDisabled), label = "rex-button-color")
    Button(onClick = onClick, enabled = enabled && !loading, interactionSource = source, modifier = modifier
        .heightIn(min = 44.dp).scale(scale), shape = RoundedCornerShape(tokens.radiusMd.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = Color(tokens.onPrimary))) {
        if (loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(tokens.onPrimary))
        else Text(label)
    }
}

@Composable
fun RexCard(tokens: RexDesignTokens, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier, shape = RoundedCornerShape(tokens.radiusLg.dp),
        colors = CardDefaults.cardColors(containerColor = Color(tokens.surface)),
        border = BorderStroke(tokens.borderThin.dp, Color(tokens.border).copy(alpha = .8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.shadowSm.dp)) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun RexSkeleton(tokens: RexDesignTokens, modifier: Modifier = Modifier, height: Int = 16) {
    val transition = rememberInfiniteTransition(label = "rex-skeleton")
    val alpha by transition.animateFloat(.35f,.75f,infiniteRepeatable(tween(900),RepeatMode.Reverse),label="skeleton-shimmer")
    Spacer(modifier.height(height.dp).background(Color(tokens.accent).copy(alpha=alpha),RoundedCornerShape(tokens.radiusSm.dp)))
}
