package com.rexaps.rexwarp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.RexWarpConnectionState as S

@Composable
fun RexWarpConnectButton(state: S, paused: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = if (paused) "Stop" else when (state) {
        S.DISCONNECTED -> "Connect"
        S.CONNECTING -> "Connecting..."
        S.CONNECTED -> "Disconnect"
        S.DISCONNECTING -> "Disconnecting..."
        S.ERROR -> "Retry"
    }
    val busy = state == S.CONNECTING || state == S.DISCONNECTING
    val connected = state == S.CONNECTED
    val c = MaterialTheme.colorScheme
    val container by animateColorAsState(
        when { connected -> c.primary; state == S.ERROR -> c.errorContainer; busy -> c.primaryContainer; else -> c.surfaceVariant },
        animationSpec = tween(360, easing = FastOutSlowInEasing), label = "connect-container"
    )
    val content by animateColorAsState(
        when { connected -> c.onPrimary; state == S.ERROR -> c.onErrorContainer; busy -> c.onPrimaryContainer; else -> c.onSurfaceVariant },
        animationSpec = tween(360, easing = FastOutSlowInEasing), label = "connect-content"
    )
    val transition = rememberInfiniteTransition(label = "connect-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(1_150, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "connect-pulse-scale"
    )
    val haloAlpha by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(tween(1_150, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "connect-halo-alpha"
    )
    val clickable = enabled && state != S.DISCONNECTING

    Box(modifier.size(190.dp), contentAlignment = Alignment.Center) {
        if (connected) {
            Box(
                Modifier.size(180.dp).scale(pulse)
                    .background(c.primary.copy(alpha = haloAlpha), CircleShape)
            )
        }
        if (busy) CircularProgressIndicator(Modifier.size(184.dp), strokeWidth = 4.dp)
        Surface(
            onClick = onClick,
            enabled = clickable,
            shape = CircleShape,
            color = container,
            contentColor = content,
            modifier = Modifier.size(156.dp)
                .scale(if (connected) pulse else 1f)
                .alpha(if (enabled) 1f else 0.5f)
                .semantics { stateDescription = state.name.lowercase().replaceFirstChar { it.uppercase() } }
        ) {
            Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }
    }
}
