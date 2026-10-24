// RexLocalAccountScreen.kt
package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun StoryComposeDialog(onDismiss: () -> Unit, onText: (String, Int) -> Unit, onVideo: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var g by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text("Buat story", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(storyGradients[g])),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text.ifBlank { "Tulis sesuatu…" },
                        color = Color.White.copy(alpha = if (text.isBlank()) .7f else 1f),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 200) text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Teks story") },
                    supportingText = { Text("${text.length}/200") },
                    shape = RoundedCornerShape(16.dp)
                )
                Text(
                    "Warna latar",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    storyGradients.forEachIndexed { i, cols ->
                        val selected = i == g
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(cols))
                                .border(if (selected) 2.dp else 0.dp, Color.White, CircleShape)
                                .clickable { g = i },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = onVideo,
                    Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Videocam, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pilih video .mp4")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = text.isNotBlank(),
                onClick = { onText(text.trim(), g) },
                shape = RoundedCornerShape(14.dp)
            ) { Text("Posting teks") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
