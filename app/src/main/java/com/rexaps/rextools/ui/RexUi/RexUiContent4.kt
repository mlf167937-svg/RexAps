package com.rexaps.rextools.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun __splitImpl_RexSkeletonPanel(media: Boolean = true, modifier: Modifier = Modifier) {
    RexPanel(modifier.shimmer()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RexSkeleton(Modifier.size(46.dp), CircleShape)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RexSkeleton(Modifier.width(140.dp).height(13.dp))
                RexSkeleton(Modifier.width(90.dp).height(11.dp))
            }
        }
        RexSkeleton(Modifier.fillMaxWidth().height(13.dp))
        if (media) RexSkeleton(Modifier.fillMaxWidth().height(190.dp), RexMd)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RexSkeleton(Modifier.weight(1f).height(50.dp), RexMd)
            RexSkeleton(Modifier.weight(1f).height(50.dp), RexMd)
        }
    }
}

@Composable
internal fun __splitImpl_RexEqualizer(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val t = rememberInfiniteTransition(label = "eq")
    val bars = listOf(520, 700, 430, 610).mapIndexed { i, ms ->
        t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(ms, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b$i")
    }
    Row(modifier.height(18.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        bars.forEach { f ->
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .graphicsLayer { scaleY = f.value; transformOrigin = TransformOrigin(0.5f, 1f) }
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/** Pindah mulus antara kondisi kosong, loading (skeleton), dan hasil. */
@Composable
internal fun __splitImpl_RexPhaseSwitch(
    phase: RexPhase,
    modifier: Modifier = Modifier,
    idle: @Composable () -> Unit,
    loading: @Composable () -> Unit,
    result: @Composable () -> Unit
) {
    AnimatedContent(
        targetState = phase,
        modifier = modifier.fillMaxWidth(),
        transitionSpec = {
            (fadeIn(tween(340, delayMillis = 100)) +
                slideInVertically(tween(480, easing = RexEase)) { it / 14 } +
                scaleIn(tween(340, delayMillis = 100), initialScale = 0.97f))
                .togetherWith(fadeOut(tween(160)))
                .using(SizeTransform(clip = false))
        },
        label = "phase"
    ) { p ->
        when (p) {
            RexPhase.Idle -> idle()
            RexPhase.Loading -> loading()
            RexPhase.Result -> result()
            RexPhase.Hidden -> Spacer(Modifier.height(0.dp))
        }
    }
}

@Composable
internal fun __splitImpl_RexRoundButton(
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

@Composable
internal fun __splitImpl_RexBackButton(onClick: () -> Unit) {
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

@Composable
internal fun __splitImpl_RexPill(
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
internal fun __splitImpl_RexHero(icon: ImageVector, title: String, description: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        RexIconTile(icon, size = 54.dp, pulse = true)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun __splitImpl_RexTonalButton(
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
internal fun __splitImpl_RexOverline(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
internal fun __splitImpl_RexOutlinedButton(
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
internal fun __splitImpl_RexSkeleton(modifier: Modifier = Modifier, shape: Shape = RexSm) {
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh))
}

@Composable
internal fun __splitImpl_hairline(alpha: Float = 0.7f) =
    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha))
