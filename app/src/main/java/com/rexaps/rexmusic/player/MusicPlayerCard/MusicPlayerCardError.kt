package com.rexaps.rexmusic.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.rexaps.rexmusic.*

@Composable
internal fun PlayerError(error: String?, onRetry: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AnimatedVisibility(visible = !error.isNullOrBlank()) {
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            shape = RoundedCornerShape(12.dp), color = scheme.errorContainer
        ) {
            Row(
                Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Error", tint = scheme.onErrorContainer)
                Spacer(Modifier.width(10.dp))
                Text(
                    error.orEmpty(), modifier = Modifier.weight(1f),
                    color = scheme.onErrorContainer, style = MaterialTheme.typography.bodySmall
                )
                TextButton(onClick = onRetry) { Text("Coba lagi") }
            }
        }
    }
}
