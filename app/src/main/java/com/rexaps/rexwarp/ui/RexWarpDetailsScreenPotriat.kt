package com.rexaps.rexwarp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.RexWarpState
import com.rexaps.rexwarp.ui.components.RexWarpDiagnosticsPanel

@Composable
internal fun RexWarpDetailsScreenPortrait(state: RexWarpState, seconds: Long?, modifier: Modifier = Modifier, onConnectClick: (() -> Unit)? = null) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Live connection information", style = MaterialTheme.typography.titleLarge)
        AnimatedVisibility(true, enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 14 }) {
            ElevatedCard(Modifier.fillMaxWidth().animateContentSize()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Session", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    RexWarpDetailsConnectionRows(state, seconds)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text("Network", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    RexWarpDetailsNetworkRows(state)
                }
            }
        }
        RexWarpDiagnosticsPanel(state, onConnectClick)
    }
}
