package com.rexaps.rextools.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_RexEmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
    val s = MaterialTheme.colorScheme
    val amp = with(LocalDensity.current) { 5.dp.toPx() }
    val t = rememberInfiniteTransition(label = "empty")
    val bob = t.animateFloat(-1f, 1f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val ring = t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "ring")
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(70.dp)
                    .graphicsLayer {
                        val r = ring.value
                        scaleX = 1f + r * 0.55f
                        scaleY = 1f + r * 0.55f
                        alpha = (1f - r) * 0.55f
                    }
                    .border(1.5.dp, s.primary, CircleShape)
            )
            Box(
                Modifier
                    .size(70.dp)
                    .graphicsLayer { translationY = bob.value * amp }
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(s.primary.copy(alpha = 0.18f), s.tertiary.copy(alpha = 0.10f))))
                    .border(1.dp, s.primary.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = s.primary, modifier = Modifier.size(28.dp))
            }
        }
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(message, style = MaterialTheme.typography.bodySmall, color = s.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun __splitImpl_RexErrorBanner(title: String, message: String) {
    val s = MaterialTheme.colorScheme
    val shake = remember { Animatable(0f) }
    LaunchedEffect(message) {
        for (v in listOf(-12f, 10f, -7f, 4f, 0f)) shake.animateTo(v, tween(55))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = shake.value }
            .clip(RexMd)
            .background(s.errorContainer)
            .border(1.dp, s.error.copy(alpha = 0.35f), RexMd)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = s.onErrorContainer)
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = s.onErrorContainer)
            Text(message, style = MaterialTheme.typography.bodySmall, color = s.onErrorContainer)
        }
    }
}

@Composable
internal fun __splitImpl_RexErrorSlot(error: String?, title: String) {
    var last by remember { mutableStateOf("") }
    SideEffect { if (error != null) last = error }
    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn() + expandVertically(spring(0.8f)),
        exit = fadeOut() + shrinkVertically()
    ) {
        RexErrorBanner(title, error ?: last)
    }
}
