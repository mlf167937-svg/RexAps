package com.rexaps.rexgit

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun PushDialogConfirmButton(onPush: () -> Unit, canCommit: Boolean) {
    Button(onClick = onPush, enabled = canCommit) {
        Icon(
            Icons.Default.CloudUpload,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text("Push")
    }
}

@Composable
internal fun PushDialogDismissButton(onDismiss: () -> Unit) {
    TextButton(onClick = onDismiss) { Text("Cancel") }
}
