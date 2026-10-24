
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.rexaps.rexgit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Location text used by the "new file / folder" sheets and dialogs. */
internal fun locationLabel(state: RexGitUiState): String {
    val repo = state.selectedRepository ?: return ""

    val relative = state.currentDirectory
        ?.removePrefix(repo.path)
        ?.trim('/')
        ?: ""

    val segments = relative.split('/').filter { it.isNotEmpty() }

    return if (segments.isEmpty()) repo.name
    else repo.name + "/" + segments.joinToString("/")
}

@Composable
internal fun Crumb(
    text: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = if (active) scheme.primary else scheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !active, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    )
}

@Composable
internal fun GuideHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
internal fun GuideText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
internal fun GuideCode(text: String) {
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

@Composable
internal fun IconTile(
    icon: ImageVector,
    tint: Color,
    size: Dp = 40.dp
) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

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
