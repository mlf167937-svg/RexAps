package com.rexaps.rexwarp.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.rexaps.rexwarp.*
import com.rexaps.rexwarp.ui.components.*
import kotlinx.coroutines.delay

@Composable
fun rememberSessionSeconds(start: Long?, active: Boolean): State<Long?> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return produceState<Long?>(null, start, active, lifecycle) {
        if (start == null || !active) { value = null; return@produceState }
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { // berhenti saat UI tidak terlihat
            while (true) { value = (SystemClock.elapsedRealtime() - start) / 1000; delay(1_000) }
        }
    }
}

@Composable
fun RexWarpHome(
    state: RexWarpState, periods: RexWarpPeriodSummaries?, hasUsage: Boolean, wide: Boolean,
    onConnectClick: () -> Unit, onOpenDetails: () -> Unit, modifier: Modifier = Modifier
) {
    val seconds by rememberSessionSeconds(state.sessionStartElapsedMs, state.connection == RexWarpConnectionState.CONNECTED)

    val hero: @Composable ColumnScope.() -> Unit = {
        state.engineUnavailableReason?.let { reason ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tunnel engine not installed", style = MaterialTheme.typography.titleSmall)
                    Text("$reason RexWARP can't connect yet. Usage history and settings still work.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            RexWarpStatusBadge(state.connection)
            RexWarpConnectButton(
                state = state.connection, paused = state.pausedReason != null,
                enabled = state.engineUnavailableReason == null || state.connection == RexWarpConnectionState.ERROR,
                onClick = onConnectClick
            )
            val message = state.error?.userMessage ?: state.pausedReason
            if (message != null) Text(
                message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
                color = if (state.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
        RexWarpStatusCard(state = state, sessionSeconds = seconds, onOpenDetails = onOpenDetails)
        RexWarpServerCard(state.info.server.takeIf { state.connection == RexWarpConnectionState.CONNECTED })
    }
    val metrics: @Composable ColumnScope.() -> Unit = {
        RexWarpSpeedCard(state.speed, connected = state.connection == RexWarpConnectionState.CONNECTED)
        RexWarpTrafficSection(periods, hasUsage)
    }

    if (wide) {
        Row(modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp), content = hero)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp), content = metrics)
        }
    } else {
        Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            hero(); metrics()
        }
    }
}
