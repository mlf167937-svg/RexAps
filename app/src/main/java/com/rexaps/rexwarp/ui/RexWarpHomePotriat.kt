package com.rexaps.rexwarp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.*

@Composable
internal fun RexWarpHomePortrait(
    state: RexWarpState,
    periods: RexWarpPeriodSummaries?,
    hasUsage: Boolean,
    onConnectClick: () -> Unit,
    onOpenDetails: () -> Unit,
    seconds: Long?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        AnimatedVisibility(visible = true, enter = fadeIn(tween(360)) + slideInVertically(tween(360)) { it / 12 }) {
            RexWarpHomeHero(state, seconds, onConnectClick, onOpenDetails)
        }
        AnimatedVisibility(visible = true, enter = fadeIn(tween(460, delayMillis = 70)) + slideInVertically(tween(460, delayMillis = 70)) { it / 14 }) {
            RexWarpHomeMetrics(state, periods, hasUsage)
        }
    }
}
