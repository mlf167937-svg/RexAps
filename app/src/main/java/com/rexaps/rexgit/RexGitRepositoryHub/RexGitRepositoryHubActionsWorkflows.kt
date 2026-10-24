@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rexaps.rexgit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun RexGitActionsWorkflowList(state: RexGitUiState) {
    val scheme = MaterialTheme.colorScheme
        if (state.workflows.isNotEmpty()) {
            Text("WORKFLOWS", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            state.workflows.forEach { workflow ->
                Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = scheme.surface), border = BorderStroke(1.dp, scheme.outlineVariant)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatusDot(workflow.state)
                        Column(Modifier.weight(1f)) {
                            Text(workflow.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("${workflow.state} · ${workflow.path}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        OpenWebButton(workflow.htmlUrl)
                    }
                }
            }
        }
}
