package com.rexaps.rextools.search

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiktokSearchTool(onBack: () -> Unit) {
    // Same shape as PinterestSearchTool — swap ApiClient.searchPinterest
    // for ApiClient.searchTiktok. Kept minimal here to avoid duplication;
    // consider extracting a shared SearchToolScaffold once you have 2+
    // of these behaving identically.
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("TikTok Search") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(20.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search TikTok") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { /* TODO: ApiClient.searchTiktok(query) */ }, modifier = Modifier.fillMaxWidth()) {
                Text("Search")
            }
        }
    }
}
