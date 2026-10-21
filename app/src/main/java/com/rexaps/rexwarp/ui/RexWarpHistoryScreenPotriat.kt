package com.rexaps.rexwarp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.RexWarpDailyUsage
import com.rexaps.rexwarp.ui.components.RexWarpEmptyState

@Composable
internal fun RexWarpHistoryScreenPortrait(
    rows: List<RexWarpDailyUsage>?,
    onSelected: (RexWarpDailyUsage) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        rows == null -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        rows.isEmpty() -> RexWarpEmptyState(
            "No usage data yet",
            "Daily totals will appear here after the tunnel has carried traffic.",
            modifier.padding(16.dp)
        )
        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Your daily activity", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 4.dp))
            }
            items(rows, key = { it.date.toString() }) { day ->
                AnimatedVisibility(visible = true, enter = fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 16 }) {
                    RexWarpHistoryRowCard(day, onClick = { onSelected(day) })
                }
            }
        }
    }
}
