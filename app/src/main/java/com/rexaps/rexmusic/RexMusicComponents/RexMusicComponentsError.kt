package com.rexaps.rexmusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ErrorCard(error: String?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    AnimatedVisibility(
        visible = !error.isNullOrBlank(),
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut()
    ) {
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = scheme.errorContainer
        ) {
            Row(
                Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Error", tint = scheme.onErrorContainer)
                Spacer(Modifier.width(10.dp))
                Text(
                    error.orEmpty(), modifier = Modifier.weight(1f),
                    color = scheme.onErrorContainer, style = MaterialTheme.typography.bodySmall
                )
                RexIconButton(
                    Icons.Default.Close, "Tutup pesan", onDismiss,
                    size = 40.dp, iconSize = 20.dp, tint = scheme.onErrorContainer
                )
            }
        }
    }
}

//  Skeleton 
