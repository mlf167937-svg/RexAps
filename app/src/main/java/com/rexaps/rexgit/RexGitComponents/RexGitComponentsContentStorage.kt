@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun StorageNotice(
    path: String,
    onGrant: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = scheme.onTertiaryContainer
            )

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    "Using private app storage",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onTertiaryContainer
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    "Downloads isn't writable, so repositories are stored in:\n$path\n\n" +
                        "Allow \"All files access\" to use /Download/RexAps instead.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onTertiaryContainer.copy(alpha = 0.85f)
                )

                TextButton(onClick = onGrant) { Text("Grant access") }
            }
        }
    }
}

@Composable
internal fun InfoPill(
    icon: ImageVector,
    text: String,
    container: Color,
    content: Color,
    compact: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = container,
        contentColor = content
    ) {
        Row(
            Modifier.padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 4.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(if (compact) 14.dp else 16.dp))

            Spacer(Modifier.width(6.dp))

            Text(
                text,
                style =
                    if (compact) MaterialTheme.typography.labelMedium
                    else MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun ChangeBadge(kind: RexGitChangeKind) {
    val scheme = MaterialTheme.colorScheme

    val (bg, fg) = when (kind) {
        RexGitChangeKind.ADDED -> scheme.primaryContainer to scheme.onPrimaryContainer
        RexGitChangeKind.MODIFIED -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        RexGitChangeKind.DELETED -> scheme.errorContainer to scheme.onErrorContainer
        RexGitChangeKind.CONFLICT -> scheme.errorContainer to scheme.onErrorContainer
        RexGitChangeKind.UNTRACKED -> scheme.secondaryContainer to scheme.onSecondaryContainer
    }

    Box(
        Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .semantics { contentDescription = kind.label },
        contentAlignment = Alignment.Center
    ) {
        Text(
            kind.symbol,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}
