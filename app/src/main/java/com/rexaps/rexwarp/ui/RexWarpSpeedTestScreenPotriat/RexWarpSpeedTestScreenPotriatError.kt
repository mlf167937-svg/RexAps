package com.rexaps.rexwarp.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun ErrorPanel(message: String) {
    val c = MaterialTheme.colorScheme
    Card(Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = c.errorContainer)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Info, null, tint = c.onErrorContainer)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Pengujian belum berhasil", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = c.onErrorContainer)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = c.onErrorContainer)
            }
        }
    }
}
