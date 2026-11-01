@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rexaps.rexgit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Shared Pull/Push controls used by both portrait and landscape layouts. */
@Composable
internal fun __splitImpl_ActionBar(
    isGit: Boolean,
    busy: Boolean,
    pulling: Boolean,
    pushing: Boolean,
    changeCount: Int,
    gitDevReady: Boolean,
    onPull: () -> Unit,
    onPush: () -> Unit,
    onUnlockPush: () -> Unit
) {
    if (!isGit) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onPull,
            enabled = !busy,
            modifier = Modifier.weight(1f)
        ) {
            if (pulling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.CloudDownload, contentDescription = null)
            }
            Spacer(Modifier.width(8.dp))
            Text(if (pulling) "Pulling…" else "Pull")
        }

        Button(
            onClick = onPush,
            enabled = !busy,
            modifier = Modifier.weight(1f)
        ) {
            if (pushing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    pushing -> "Pushing…"
                    changeCount > 0 -> "Push ($changeCount)"
                    else -> "Push"
                }
            )
        }

        IconButton(
            onClick = onUnlockPush,
            enabled = !busy,
            modifier = Modifier.semantics {
                contentDescription = if (gitDevReady) "GitDev settings" else "Configure GitDev"
            }
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                tint = if (gitDevReady) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
