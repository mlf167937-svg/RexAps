package com.rexaps.rexwarp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
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
internal fun RexWarpDetailsScreenLandscape(state: RexWarpState, seconds: Long?, modifier: Modifier = Modifier, onConnectClick: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedVisibility(
            visible = true,
            modifier = Modifier.weight(0.78f).fillMaxHeight(),
            enter = fadeIn(tween(320)) + slideInHorizontally(tween(320)) { it / 18 }
        ) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Session", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        RexWarpDetailsConnectionRows(state, seconds)
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = true,
            modifier = Modifier.weight(1.22f).fillMaxHeight(),
            enter = fadeIn(tween(420, delayMillis = 70)) + slideInHorizontally(tween(420, delayMillis = 70)) { it / 18 }
        ) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Network details", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        RexWarpDetailsNetworkRows(state)
                    }
                }
                Spacer(Modifier.height(12.dp))
                RexWarpDiagnosticsPanel(state, onConnectClick)
            }
        }
    }
}
