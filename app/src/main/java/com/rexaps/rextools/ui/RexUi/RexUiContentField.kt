package com.rexaps.rextools.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ═════════════════════ ICON TILE & HERO ═════════════════════ */

@Composable
fun RexIconTile(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 48.dp, pulse: Boolean = false) {
    val s = MaterialTheme.colorScheme
    val glow = rememberInfiniteTransition(label = "tile")
        .animateFloat(0.15f, 0.5f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "g")
    Box(
        modifier
            .size(size)
            .drawBehind {
                if (pulse) {
                    drawCircle(
                        Brush.radialGradient(listOf(s.primary.copy(alpha = glow.value), Color.Transparent)),
                        radius = this.size.minDimension * 0.95f
                    )
                }
            }
            .clip(RexSm)
            .background(Brush.linearGradient(listOf(s.primary.copy(alpha = 0.24f), s.tertiary.copy(alpha = 0.14f))))
            .border(1.dp, Brush.linearGradient(listOf(s.primary.copy(alpha = 0.6f), s.tertiary.copy(alpha = 0.15f))), RexSm),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = s.primary, modifier = Modifier.size(size * 0.46f))
    }
}

@Composable
fun RexHero(icon: ImageVector, title: String, description: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        RexIconTile(icon, size = 54.dp, pulse = true)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RexOverline(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/* ═════════════════════ INPUT ═════════════════════ */

@Composable
fun RexField(
    value: String,
    onChange: (String) -> Unit,
    label: String?,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    icon: ImageVector? = null,
    clearable: Boolean = true,
    shape: Shape = RexMd,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val s = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val glow by animateDpAsState(if (focused) 16.dp else 0.dp, tween(320), label = "glow")
    val trailing: (@Composable () -> Unit)? = if (clearable) {
        {
            AnimatedVisibility(
                visible = value.isNotEmpty(),
                enter = fadeIn() + scaleIn(initialScale = 0.6f),
                exit = fadeOut() + scaleOut(targetScale = 0.6f)
            ) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Hapus teks")
                }
            }
        }
    } else null

    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = icon?.let { { Icon(it, contentDescription = null) } },
        trailingIcon = trailing,
        singleLine = true,
        shape = shape,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = source,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = s.surfaceContainerLow,
            unfocusedContainerColor = s.surfaceContainerLow,
            focusedBorderColor = s.primary,
            unfocusedBorderColor = s.outlineVariant,
            focusedLeadingIconColor = s.primary,
            unfocusedLeadingIconColor = s.onSurfaceVariant,
            focusedLabelColor = s.primary,
            cursorColor = s.primary
        ),
        modifier = modifier
            .fillMaxWidth()
            .shadow(glow, shape, ambientColor = s.primary, spotColor = s.primary)
    )
}

/* ═════════════════════ PILL, CHIP, STAT ═════════════════════ */

@Composable
fun RexPill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    content: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector? = null
) {
    Row(
        modifier
            .clip(CircleShape)
            .background(container)
            .border(1.dp, content.copy(alpha = 0.2f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = content, maxLines = 1)
    }
}

@Composable
fun RexFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val s = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val container by animateColorAsState(
        if (selected) s.primary else s.surfaceContainerHigh.copy(alpha = 0.6f), tween(260), label = "bg"
    )
    val content by animateColorAsState(if (selected) s.onPrimary else s.onSurfaceVariant, tween(260), label = "fg")
    val borderColor by animateColorAsState(if (selected) Color.Transparent else s.outlineVariant, tween(260), label = "bd")
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = Modifier.pressScale(source, 0.93f),
        shape = CircleShape,
        color = container,
        border = BorderStroke(1.dp, borderColor),
        interactionSource = source
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = content,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}
