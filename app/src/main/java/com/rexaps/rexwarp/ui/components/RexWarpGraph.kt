package com.rexaps.rexwarp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rexaps.rexwarp.RexWarpBucket

/**
 * Interactive history chart. It only draws buckets marked as observed; an interval without a
 * saved sample is kept visibly unavailable instead of being presented as zero traffic.
 */
@Composable
fun RexWarpGraph(series: List<RexWarpBucket>, modifier: Modifier = Modifier) { __RexWarpGraphContent(series, modifier) }
