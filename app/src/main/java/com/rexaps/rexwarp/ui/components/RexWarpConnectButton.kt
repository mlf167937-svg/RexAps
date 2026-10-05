package com.rexaps.rexwarp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.RexWarpConnectionState as S

@Composable
fun RexWarpConnectButton(state: S, paused: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = if (paused) "Stop" else when (state) {
        S.DISCONNECTED -> "Connect"; S.CONNECTING -> "Connecting..."; S.CONNECTED -> "Disconnect"
        S.DISCONNECTING -> "Disconnecting..."; S.ERROR -> "Retry"
    }
    val busy = state == S.CONNECTING || state == S.DISCONNECTING
    val c = MaterialTheme.colorScheme
    val container by animateColorAsState(
        when { state == S.CONNECTED -> c.primary; state == S.ERROR -> c.errorContainer; busy -> c.primaryContainer; else -> c.surfaceVariant },
        tween(300), label = "container"
    )
    val content by animateColorAsState(
        when { state == S.CONNECTED -> c.onPrimary; state == S.ERROR -> c.onErrorContainer; busy -> c.onPrimaryContainer; else -> c.onSurfaceVariant },
        tween(300), label = "content"
    )
    val clickable = enabled && state != S.DISCONNECTING
    Box(modifier.size(184.dp), contentAlignment = Alignment.Center) {
        if (busy) CircularProgressIndicator(Modifier.fillMaxSize(), strokeWidth = 4.dp)
        Surface(
            onClick = onClick, enabled = clickable, shape = CircleShape, color = container, contentColor = content,
            modifier = Modifier.size(156.dp).alpha(if (enabled) 1f else 0.5f)
                .semantics { stateDescription = state.name.lowercase().replaceFirstChar { it.uppercase() } }
        ) {
            Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }
    }
}