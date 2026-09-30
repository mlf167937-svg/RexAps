package com.rexaps.rexmanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import com.rexaps.rexmanager.OperationProgress

@Composable
fun RexManagerProgressDialog(progress: OperationProgress, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(progress.title) },
        text = {
            Column {
                if (progress.currentName.isNotEmpty()) {
                    Text(progress.currentName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(12.dp))
                }
                val f = progress.fraction
                if (f == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                else {
                    LinearProgressIndicator(progress = { f }, modifier = Modifier.fillMaxWidth())
                    Text("${(f * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
    )
}
