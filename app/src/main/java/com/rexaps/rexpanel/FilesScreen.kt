package com.rexaps.rexpanel

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FilesScreen(b: FileBrowserState, modifier: Modifier = Modifier) {
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(b.notice) {
        b.notice?.let {
            snackbar.showSnackbar(it)
            b.notice = null
        }
    }

    Box(modifier) {
        val p = b.preview
        if (p != null) FilePreview(b, p, Modifier.fillMaxSize()) else FileBrowser(b, Modifier.fillMaxSize())

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            b.transfers.ui?.let { TransferBar(it, onCancel = { b.transfers.cancel() }) }
            SnackbarHost(snackbar)
        }
    }
}
