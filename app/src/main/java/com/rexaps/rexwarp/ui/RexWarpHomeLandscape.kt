package com.rexaps.rexwarp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.*

@Composable
internal fun RexWarpHomeLandscape(
    state: RexWarpState,
    periods: RexWarpPeriodSummaries?,
    hasUsage: Boolean,
    onConnectClick: () -> Unit,
    onOpenDetails: () -> Unit,
    seconds: Long?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(
            Modifier.weight(1.02f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(visible = true, enter = fadeIn(tween(350)) + slideInHorizontally(tween(350)) { it / 16 }) {
                RexWarpHomeHero(state, seconds, onConnectClick, onOpenDetails)
            }
        }
        Column(
            Modifier.weight(0.98f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(visible = true, enter = fadeIn(tween(430, delayMillis = 65)) + slideInHorizontally(tween(430, delayMillis = 65)) { it / 18 }) {
                RexWarpHomeMetrics(state, periods, hasUsage)
            }
        }
    }
}
