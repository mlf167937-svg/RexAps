package com.rexaps.rexwarp.ui

import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = (SystemClock.elapsedRealtime() - start) / 1000
                delay(1_000)
            }
        }
    }
}

@Composable
fun RexWarpHome(
    state: RexWarpState, periods: RexWarpPeriodSummaries?, hasUsage: Boolean, wide: Boolean,
    onConnectClick: () -> Unit, onOpenDetails: () -> Unit, modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || wide
    val seconds by rememberSessionSeconds(
        state.sessionStartElapsedMs,
        state.connection == RexWarpConnectionState.CONNECTED
    )
    if (landscape) {
        RexWarpHomeLandscape(state, periods, hasUsage, onConnectClick, onOpenDetails, seconds, modifier)
    } else {
        RexWarpHomePortrait(state, periods, hasUsage, onConnectClick, onOpenDetails, seconds, modifier)
    }
}

@Composable
internal fun RexWarpHomeHero(
    state: RexWarpState,
    seconds: Long?,
    onConnectClick: () -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        state.engineUnavailableReason?.let { reason ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tunnel engine not installed", style = MaterialTheme.typography.titleSmall)
                    Text("$reason RexWARP can't connect yet. Usage history and settings still work.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RexWarpStatusBadge(state.connection)
            RexWarpConnectButton(
                state = state.connection,
                paused = state.pausedReason != null,
                enabled = state.engineUnavailableReason == null || state.connection == RexWarpConnectionState.ERROR,
                onClick = onConnectClick
            )
            val message = state.error?.userMessage ?: state.pausedReason
            if (message != null) Text(
                message,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
        RexWarpStatusCard(state = state, sessionSeconds = seconds, onOpenDetails = onOpenDetails)
        RexWarpServerCard(state.info.server.takeIf { state.connection == RexWarpConnectionState.CONNECTED })
    }
}

@Composable
internal fun RexWarpHomeMetrics(
    state: RexWarpState,
    periods: RexWarpPeriodSummaries?,
    hasUsage: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RexWarpSpeedCard(state.speed, connected = state.connection == RexWarpConnectionState.CONNECTED)
        RexWarpTrafficSection(periods, hasUsage)
    }
}
