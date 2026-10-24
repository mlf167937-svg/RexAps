package com.rexaps.rexpanel

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun StatTile(
    title: String,
    value: String,
    sub: String,
    modifier: Modifier
) {
    val colors =
        MaterialTheme.colorScheme

    val shape =
        RoundedCornerShape(20.dp)

    Column(
        modifier
            .clip(shape)
            .background(
                colors.surfaceVariant
            )
            .border(
                1.dp,
                colors.outlineVariant,
                shape
            )
            .padding(14.dp),

        verticalArrangement =
            Arrangement.spacedBy(2.dp)
    ) {

        Text(
            title,
            style =
                MaterialTheme.typography
                    .labelMedium,
            color =
                colors.onSurfaceVariant
        )

        Text(
            value,
            style =
                MaterialTheme.typography
                    .titleMedium,
            color =
                colors.primary,
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis
        )

        if (sub.isNotEmpty()) {

            Text(
                sub,
                style =
                    MaterialTheme.typography
                        .labelSmall,
                color =
                    colors.onSurfaceVariant,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}
