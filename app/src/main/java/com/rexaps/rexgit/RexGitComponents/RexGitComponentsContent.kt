@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.items

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
internal fun ChangesPanel(
    changes: List<RexGitChange>,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        color = scheme.surface,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = if (expanded) "Hide changes" else "Show changes",
                        onClick = onToggle
                    )
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Changes (${changes.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    if (expanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant
                )
            }

            if (expanded) {
                LazyColumn(
                    Modifier.heightIn(max = 200.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(changes.take(300), key = { it.kind.name + it.path }) { change ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ChangeBadge(change.kind)

                            Spacer(Modifier.width(10.dp))

                            Text(
                                change.path,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (changes.size > 300) {
                        item(key = "more") {
                            Text(
                                "+ ${changes.size - 300} more",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ============================================================
// Cards & rows
// ============================================================

@Composable
internal fun WorkspaceCard(
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
internal fun FileRow(
    file: RexGitFile,
    change: RexGitChangeKind?,
    hasChangesInside: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val style = fileStyle(file, scheme)

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = if (file.isDirectory) "Open folder" else "Open file",
                onClick = onClick
            )
            .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(style.icon, style.color)

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                file.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                if (file.isDirectory) "Folder" else formatSize(file.size),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }

        if (change != null) {
            Spacer(Modifier.width(8.dp))
            ChangeBadge(change)
        } else if (hasChangesInside) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(scheme.tertiary)
                    .semantics { contentDescription = "Contains changes" }
            )
        }

        IconButton(onClick = onMore) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Actions for ${file.name}",
                tint = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun GuideText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}
