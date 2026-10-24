package com.rexaps.rexnux

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_RexNuxNoDistroContent(
    modifier: Modifier = Modifier,
    onDownload: () -> Unit,
    error: String?
) {
    Column(
        modifier = modifier
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "RexNux belum siap",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Belum ada distro Linux yang terinstall.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = "Download Alpine Linux atau Arch Linux " +
                    "untuk mulai menggunakan RexNux.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!error.isNullOrBlank()) {
            Spacer(
                modifier = Modifier.height(16.dp)
            )

            RexNuxErrorMessage(
                message = error
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Button(
            onClick = onDownload
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null
            )

            Spacer(
                modifier = Modifier.size(8.dp)
            )

            Text("Download Distro")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun __splitImpl_RexNuxHomeScreen(
    state: RexNuxUiState,
    onExit: () -> Unit,
    onDownload: () -> Unit,
    onSettings: () -> Unit,
    onOpenRexNux: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.93f),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "RexNux",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onExit
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali ke RexAps"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSettings
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        if (state.installedDistro == null) {
            RexNuxNoDistroContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                onDownload = onDownload,
                error = state.error
            )
        } else {
            RexNuxInstalledContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                distro = state.installedDistro,
                error = state.error,
                onOpen = onOpenRexNux
            )
        }
    }
}
