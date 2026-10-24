package com.rexaps.rexmusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun rememberPressScale(source: MutableInteractionSource, pressed: Float = 0.96f): State<Float> {
    val isPressed by source.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
}

//  Basic building blocks 

/** Tombol ikon bulat dengan ukuran pasti (tidak pernah melebar / bertabrakan). */
@Composable
fun RexIconButton(
    icon: ImageVector,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    background: Color = Color.Unspecified,
    enabled: Boolean = true
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .then(if (background.isSpecified) Modifier.background(background) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) tint else tint.copy(alpha = 0.38f),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title, style = MaterialTheme.typography.titleLarge,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/** Saklar Online / Offline berbentuk pill. */
@Composable
fun ModeChip(offline: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val fg by animateColorAsState(
        if (offline) scheme.tertiary else scheme.onSurfaceVariant, label = "chipFg"
    )
    val bg by animateColorAsState(
        if (offline) scheme.tertiary.copy(alpha = 0.16f) else scheme.onSurface.copy(alpha = 0.08f),
        label = "chipBg"
    )
    Row(
        modifier.height(36.dp).clip(CircleShape).background(bg)
            .clickable(role = Role.Switch, onClick = onToggle)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            if (offline) Icons.Default.CloudOff else Icons.Default.Cloud,
            contentDescription = null, tint = fg, modifier = Modifier.size(16.dp)
        )
        Text(
            if (offline) "Offline" else "Online", color = fg,
            style = MaterialTheme.typography.labelLarge, maxLines = 1
        )
    }
}

/** Header untuk tab Cari & Koleksi. */
@Composable
fun TabHeader(title: String, offline: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title, style = MaterialTheme.typography.headlineMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        ModeChip(offline, onToggle)
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(80.dp).clip(CircleShape).background(scheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            message, style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant, textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun ErrorCard(error: String?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    AnimatedVisibility(
        visible = !error.isNullOrBlank(),
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut()
    ) {
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = scheme.errorContainer
        ) {
            Row(
                Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Error", tint = scheme.onErrorContainer)
                Spacer(Modifier.width(10.dp))
                Text(
                    error.orEmpty(), modifier = Modifier.weight(1f),
                    color = scheme.onErrorContainer, style = MaterialTheme.typography.bodySmall
                )
                RexIconButton(
                    Icons.Default.Close, "Tutup pesan", onDismiss,
                    size = 40.dp, iconSize = 20.dp, tint = scheme.onErrorContainer
                )
            }
        }
    }
}

//  Skeleton 

@Composable
fun SkeletonList(modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val x by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f, targetValue = 900f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmerX"
    )
    val brush = Brush.linearGradient(
        colors = listOf(base.copy(alpha = 0.35f), base.copy(alpha = 0.9f), base.copy(alpha = 0.35f)),
        start = Offset(x - 300f, 0f), end = Offset(x, 0f)
    )
    Column(modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(7) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(brush))
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                    Box(Modifier.fillMaxWidth(0.4f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                }
            }
        }
    }
}
