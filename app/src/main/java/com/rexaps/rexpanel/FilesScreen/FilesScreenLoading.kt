package com.rexaps.rexpanel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun SkeletonRows() {
    val c = MaterialTheme.colorScheme.surfaceVariant
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        repeat(7) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(c))
                Spacer(Modifier.width(12.dp))
                Column {
                    Box(Modifier.width(160.dp).height(14.dp).clip(RoundedCornerShape(6.dp)).background(c))
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.width(100.dp).height(10.dp).clip(RoundedCornerShape(6.dp)).background(c))
                }
            }
        }
    }
}

@Composable
internal fun DownloadProgress(done: Long, total: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (total > 0) {
            CircularProgressIndicator(progress = (done.toFloat() / total).coerceIn(0f, 1f), color = Color.White)
        } else {
            CircularProgressIndicator(color = Color.White)
        }
        Spacer(Modifier.height(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
        if (total > 0) {
            Text(
                "${fmtBytes(done)} / ${fmtBytes(total)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
