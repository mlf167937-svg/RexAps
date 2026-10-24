@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

/** Pull / Push bar. Standard Push is available even when GitDev is disabled. */
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
    val scheme = MaterialTheme.colorScheme

    Surface(color = scheme.surface) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(scheme.outlineVariant)
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onPull,
                    enabled = isGit && !busy,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, scheme.outline),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                ) {
                    if (pulling) ButtonSpinner()
                    else Icon(Icons.Default.CloudDownload, contentDescription = null)

                    Spacer(Modifier.width(8.dp))
                    Text(if (pulling) "Pulling" else "Pull")
                }

                GradientButton(
                    text = when {
                        pushing -> "Pushing"
                        changeCount > 0 -> "Push ($changeCount)"
                        else -> "Push"
                    },
                    icon = Icons.Default.CloudUpload,
                    onClick = onPush,
                    enabled = isGit && !busy,
                    loading = pushing,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
