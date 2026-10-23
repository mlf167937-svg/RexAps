package com.rexaps.rexwarp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.RexWarpBucket
import com.rexaps.rexwarp.RexWarpDailyUsage
import com.rexaps.rexwarp.ui.components.RexWarpEmptyState
import com.rexaps.rexwarp.ui.components.RexWarpGraph

@Composable
internal fun RexWarpHistoryScreenLandscape(
    rows: List<RexWarpDailyUsage>?,
    buckets: List<RexWarpBucket>,
    graphDays: Int,
    onGraphDays: (Int) -> Unit,
    onSelected: (RexWarpDailyUsage) -> Unit,
    modifier: Modifier = Modifier,
    loadingHourly: Boolean = false,
    rangeLabel: String? = null
) {
    if (rows == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(
        modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (rows.isEmpty()) RexWarpEmptyState(
            "Belum ada data pada rentang ini",
            "Statistik harian muncul setelah trafik direkam. Ubah periode untuk melihat rentang lain."
        )
        RexWarpHistoryGraphHeader(graphDays, onGraphDays, rangeLabel)
        when {
            loadingHourly -> Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            buckets.isEmpty() -> RexWarpEmptyState("Belum ada data pada rentang ini", "Pilih rentang lain atau lanjutkan pencatatan trafik.")
            buckets.none { it.hasData } -> RexWarpEmptyState("Sampel belum tersedia", "Data tidak direkam pada interval yang dipilih. Tidak ada angka contoh yang ditambahkan.")
            else -> ElevatedCard(Modifier.fillMaxWidth()) { RexWarpGraph(buckets, Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) }
        }
        Text(if (graphDays == -24) "Trafik hari ini" else "Hari dengan trafik", style = MaterialTheme.typography.titleMedium)
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
