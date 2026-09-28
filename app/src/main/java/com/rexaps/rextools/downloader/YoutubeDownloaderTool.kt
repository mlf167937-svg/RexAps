package com.rexaps.rextools.downloader

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubeDownloaderTool(onBack: () -> Unit) {
    var url by remember { mutableStateOf("") }
    var asAudio by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("YouTube Downloader") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(20.dp)
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Paste YouTube link") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Switch(checked = asAudio, onCheckedChange = { asAudio = it })
                Spacer(modifier = Modifier.width(8.dp))
                Text("Audio only")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { /* TODO: ApiClient.fetchDownloadInfo(url) */ }, modifier = Modifier.fillMaxWidth()) {
                Text("Fetch download link")
            }
        }
    }
}
