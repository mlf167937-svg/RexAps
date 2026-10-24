package com.rexaps.rexnux

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RexNuxHomeScreen(
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

/*
 * ------------------------------------------------------------
 * NO DISTRO
 * ------------------------------------------------------------
 */

@Composable
internal fun RexNuxNoDistroContent(
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

/*
 * ------------------------------------------------------------
 * INSTALLED DISTRO
 * ------------------------------------------------------------
 */

@Composable
internal fun RexNuxInstalledContent(
    modifier: Modifier = Modifier,
    distro: RexNuxDistro,
    error: String?,
    onOpen: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Text(
                text = "Linux Environment",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = distro.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Text(
                                text = distro.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme
                                    .onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "Installed",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )

                    Button(
                        onClick = onOpen,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open RexNux")
                    }
                }
            }
        }

        if (!error.isNullOrBlank()) {
            item {
                RexNuxErrorMessage(
                    message = error
                )
            }
        }
    }
}

/*
 * ------------------------------------------------------------
 * DOWNLOAD DISTRO
 * ------------------------------------------------------------
 */
