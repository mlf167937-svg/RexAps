@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_WorkspaceCard(
    total: Int,
    gitCount: Int,
    path: String
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(scheme.primaryContainer, scheme.secondaryContainer)
                )
            )
            .border(1.dp, scheme.primary.copy(alpha = 0.25f), shape)
            .padding(20.dp)
    ) {
        Text(
            "Workspace",
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onPrimaryContainer.copy(alpha = 0.7f)
        )

        Text(
            "$total",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = scheme.onPrimaryContainer
        )

        Text(
            if (total == 1) "repository" else "repositories",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onPrimaryContainer.copy(alpha = 0.8f)
        )

        Spacer(Modifier.height(14.dp))

        InfoPill(
            icon = Icons.Default.CallSplit,
            text = "$gitCount Git",
            container = scheme.surface.copy(alpha = 0.55f),
            content = scheme.onSurface
        )

        if (path.isNotBlank()) {
            Spacer(Modifier.height(12.dp))

            Text(
                path,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = scheme.onPrimaryContainer.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun __splitImpl_StorageNotice(
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
internal fun __splitImpl_GradientButton(
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

@Composable
internal fun __splitImpl_ChangeBadge(kind: RexGitChangeKind) {
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

@Composable
internal fun __splitImpl_InfoPill(
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
internal fun __splitImpl_GuideCode(text: String) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = scheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = scheme.onSurfaceVariant,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        )
    }
}
