@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

/** Pull / Push bar. Standard Push is available even when GitDev is disabled. */
@Composable
internal fun ActionBar(
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

@Composable
internal fun GradientButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme

    val brush: Brush =
        if (enabled || loading) Brush.linearGradient(listOf(scheme.primary, scheme.secondary))
        else SolidColor(scheme.onSurface.copy(alpha = 0.12f))

    val content =
        if (enabled || loading) scheme.onPrimary
        else scheme.onSurface.copy(alpha = 0.38f)

    Row(
        modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = content
            )
        } else {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(8.dp))

        Text(text, color = content, style = MaterialTheme.typography.labelLarge)
    }
}
