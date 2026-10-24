
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.rexaps.rexgit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun TappablePill(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        content()
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

// ============================================================
// Utilities
// ============================================================
