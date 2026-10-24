
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.rexaps.rexgit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Person

internal fun accountSuffix(state: RexGitUiState): String {
    return if (state.accounts.size > 1) "  ·  ${state.accounts.size} accounts" else ""
}

/** Small chips to switch between the connected accounts. */
@Composable
internal fun AccountSwitcher(
    accounts: List<String>,
    active: Int,
    onSwitch: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        accounts.forEachIndexed { index, name ->
            val selected = index == active

            Surface(
                onClick = { onSwitch(index) },
                shape = RoundedCornerShape(12.dp),
                color = if (selected) scheme.primaryContainer else scheme.surfaceVariant,
                contentColor = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                border = BorderStroke(
                    1.dp,
                    if (selected) scheme.primary.copy(alpha = 0.4f) else scheme.outlineVariant
                )
            ) {
                Row(
                    Modifier
                        .heightIn(min = 40.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (selected) Icons.Default.CheckCircle else Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        "@$name",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun AccountRow(
    name: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = if (active) scheme.primaryContainer.copy(alpha = 0.55f) else scheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (active) scheme.primary.copy(alpha = 0.45f) else scheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .heightIn(min = 64.dp)
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(scheme.primary, scheme.secondary))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onPrimary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "@$name",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    if (active) "In use" else "Tap to use this account",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (active) scheme.primary else scheme.onSurfaceVariant
                )
            }

            if (active) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "In use",
                    tint = scheme.primary
                )
            }

            IconButton(onClick = onRemove, enabled = enabled) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove @$name",
                    tint = scheme.error.copy(alpha = if (enabled) 1f else 0.38f)
                )
            }
        }
    }
}
