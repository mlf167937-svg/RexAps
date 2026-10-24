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

/*
 * ------------------------------------------------------------
 * HOME
 * ------------------------------------------------------------
 */

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
 * DISTRO CARD
 * ------------------------------------------------------------
 */

@Composable
internal fun RexNuxDistroCard(
    distro: RexNuxDistro,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = distro.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = distro.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selected) {
                Spacer(
                    modifier = Modifier.size(12.dp)
                )

                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
