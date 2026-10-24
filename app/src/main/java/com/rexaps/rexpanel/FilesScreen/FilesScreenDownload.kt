package com.rexaps.rexpanel

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_DownloadProgress(done: Long, total: Long, label: String) {
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
