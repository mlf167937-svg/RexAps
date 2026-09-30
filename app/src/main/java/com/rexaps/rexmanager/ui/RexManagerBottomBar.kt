package com.rexaps.rexmanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rexaps.rexmanager.Clipboard

@Composable
fun RexManagerBottomBar(clipboard: Clipboard, onPaste: () -> Unit, onCancel: () -> Unit) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val n = clipboard.paths.size
            Text(
                "${if (clipboard.isMove) "Moving" else "Copying"} $n item${if (n == 1) "" else "s"}",
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onCancel) { Text("Cancel") }
            Button(onClick = onPaste) { Text("Paste") }
        }
    }
}
