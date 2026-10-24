
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.rexaps.rexgit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun HomeHeader(
    state: RexGitUiState,
    onRefresh: () -> Unit,
    onGithub: () -> Unit,
    compact: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                start = 20.dp,
                end = 16.dp,
                top = if (compact) 8.dp else 12.dp,
                bottom = if (compact) 8.dp else 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RexGitLogo()

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text("RexGit", style = MaterialTheme.typography.headlineSmall)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.isGithubConnected) scheme.secondary else scheme.outline
                        )
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    if (state.isGithubConnected) "@${state.githubUsername}" + accountSuffix(state)
                    else "GitHub not connected",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        HeaderIconButton(
            icon = Icons.Default.Refresh,
            description = "Refresh repositories",
            onClick = onRefresh
        )

        Spacer(Modifier.width(8.dp))

        HeaderIconButton(
            icon = Icons.Default.Cloud,
            description =
                if (state.isGithubConnected) "GitHub account, connected"
                else "Connect GitHub",
            onClick = onGithub,
            tint = if (state.isGithubConnected) scheme.primary else scheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun RepositoryHeader(
    repo: RexGitRepository,
    busy: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    compact: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = if (compact) 8.dp else 12.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderIconButton(
            icon = Icons.Default.ArrowBack,
            description = "Back to repositories",
            onClick = onBack
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                repo.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!repo.remoteUrl.isNullOrBlank()) {
                Text(
                    repo.remoteUrl.removePrefix("https://"),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        HeaderIconButton(
            icon = Icons.Default.Refresh,
            description = "Refresh status",
            onClick = onRefresh,
            enabled = !busy
        )
    }
}

@Composable
internal fun HeaderIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = description,
                tint = if (enabled) tint else tint.copy(alpha = 0.38f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
