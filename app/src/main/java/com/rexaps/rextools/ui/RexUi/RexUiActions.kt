package com.rexaps.rextools.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun RexBackButton(onClick: () -> Unit) {
    val s = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = Modifier.padding(start = 12.dp).size(40.dp).pressScale(source, 0.88f),
        shape = CircleShape,
        color = s.surfaceContainerHigh.copy(alpha = 0.8f),
        border = hairline(0.9f),
        interactionSource = source
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Kembali", modifier = Modifier.size(20.dp))
        }
    }
}

/* ═════════════════════ BUTTONS ═════════════════════ */

@Composable
fun RexPrimaryButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    loadingText: String = "Memproses…",
    enabled: Boolean = true
) {
    val s = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val canClick = enabled && !loading
    val glow by animateDpAsState(if (enabled) 18.dp else 0.dp, tween(300), label = "glow")
    val fg by animateColorAsState(if (enabled) s.onPrimary else s.onSurfaceVariant, label = "fg")
    val sweep = rememberInfiniteTransition(label = "sweep")
        .animateFloat(-0.6f, 1.6f, infiniteRepeatable(tween(2800, easing = LinearEasing)), label = "x")
    val brush = Brush.linearGradient(listOf(s.primary, s.tertiary))

    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .pressScale(source, 0.97f)
            .shadow(glow, RexMd, ambientColor = s.primary, spotColor = s.primary)
            .clip(RexMd)
            .then(if (enabled) Modifier.background(brush) else Modifier.background(s.surfaceContainerHighest))
            .drawWithContent {
                drawContent()
                if (canClick) {
                    val w = size.width
                    val x = w * sweep.value
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent),
                            start = Offset(x - w * 0.25f, 0f),
                            end = Offset(x, size.height)
                        )
                    )
                }
            }
            .clickable(interactionSource = source, indication = null, enabled = canClick, role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = loading,
            transitionSpec = {
                (fadeIn(tween(200)) + scaleIn(initialScale = 0.8f)) togetherWith (fadeOut(tween(120)) + scaleOut(targetScale = 0.8f))
            },
            label = "btn"
        ) { isLoading ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = fg)
                    Spacer(Modifier.width(10.dp))
                    Text(loadingText, color = fg, fontWeight = FontWeight.SemiBold)
                } else {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(text, color = fg, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun RexButtonBase(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier,
    container: Color,
    content: Color,
    border: BorderStroke?,
    enabled: Boolean,
    loading: Boolean
) {
    val source = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val alphaAnim by animateFloatAsState(if (enabled) 1f else 0.4f, label = "a")
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled && !loading,
        modifier = modifier.height(50.dp).pressScale(source).graphicsLayer { this.alpha = alphaAnim },
        shape = RexMd,
        color = container,
        contentColor = content,
        border = border,
        interactionSource = source
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = loading,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "btnBase"
            ) { isLoading ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLoading) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                    } else {
                        if (icon != null) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun RexTonalButton(
    text: String, icon: ImageVector?, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false
) {
    val s = MaterialTheme.colorScheme
    RexButtonBase(
        text, icon, onClick, modifier,
        container = s.primary.copy(alpha = 0.14f), content = s.primary,
        border = BorderStroke(1.dp, s.primary.copy(alpha = 0.30f)),
        enabled = enabled, loading = loading
    )
}

@Composable
fun RexOutlinedButton(
    text: String, icon: ImageVector?, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true
) {
    val s = MaterialTheme.colorScheme
    RexButtonBase(
        text, icon, onClick, modifier,
        container = Color.Transparent, content = s.onSurface,
        border = hairline(0.95f), enabled = enabled, loading = false
    )
}

@Composable
fun RexRoundButton(
    icon: ImageVector, contentDescription: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, size: Dp = 52.dp
) {
    val s = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier.size(size).pressScale(source, 0.88f),
        shape = CircleShape,
        color = s.surfaceContainerHigh,
        border = hairline(0.9f),
        interactionSource = source
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription)
        }
    }
}
