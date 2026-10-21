package com.rexaps.rexwarp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.RexWarpDailyUsage
import com.rexaps.rexwarp.ui.components.RexWarpEmptyState

@Composable
internal fun RexWarpHistoryScreenLandscape(
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
        else -> Column(
            modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Your daily activity", style = MaterialTheme.typography.titleLarge)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 285.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rows, key = { it.date.toString() }) { day ->
                    AnimatedVisibility(visible = true, enter = fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 20 }) {
                        RexWarpHistoryRowCard(day, onClick = { onSelected(day) })
                    }
                }
            }
        }
    }
}
