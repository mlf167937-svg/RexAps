package com.rexaps.rextools.search

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.rexaps.rextools.downloader.ImageDownloader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinterestScreen(
    onBack: () -> Unit = {},
    vm: PinterestViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pinterest") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("cari gambar pinterest") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { vm.search(query) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.loading
            ) {
                Text(if (state.loading) "loading..." else "cari")
            }

            Spacer(Modifier.height(16.dp))

            if (state.error != null) {
                Text("error: ${state.error}", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            if (state.images.isNotEmpty()) {
                val currentUrl = state.images[state.currentIndex]

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    AsyncImage(
                        model = currentUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    "gambar ${state.currentIndex + 1} / ${state.images.size}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { vm.prev() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("prev")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val res = ImageDownloader.download(context, currentUrl)
                                res.onSuccess {
                                    Toast.makeText(context, "tersimpan", Toast.LENGTH_SHORT).show()
                                }.onFailure {
                                    Toast.makeText(context, "gagal: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("download")
                    }

                    OutlinedButton(
                        onClick = { vm.next() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("next")
                    }
                }
            } else if (!state.loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("hasil muncul di sini")
                }
            }
        }
    }
}
