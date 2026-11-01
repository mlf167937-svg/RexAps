package com.rexaps.rexgit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun GitOperationProgressCard(
    title: String,
    progress: GitProgressState,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth().widthIn(max = 620.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(progress.percent?.let { "$it%" } ?: "Working", style = MaterialTheme.typography.labelMedium, color = scheme.primary)
            }
            if (progress.percent != null) {
                LinearProgressIndicator(
                    progress = progress.percent.coerceIn(0, 100) / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Text(
                progress.taskTitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (progress.totalTasks > 0) {
                Text(
                    "Git task ${progress.completedTasks.coerceAtMost(progress.totalTasks)}/${progress.totalTasks} · estimated task progress",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}
