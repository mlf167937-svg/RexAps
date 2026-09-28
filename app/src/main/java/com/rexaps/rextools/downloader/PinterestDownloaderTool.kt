package com.rexaps.rextools.downloader

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinterestDownloaderTool(onBack: () -> Unit) {
    var url by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Pinterest Downloader") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(20.dp)
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Paste Pinterest link") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { /* TODO: ApiClient.fetchDownloadInfo(url) */ }, modifier = Modifier.fillMaxWidth()) {
                Text("Fetch download link")
            }
        }
    }
}
