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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RexNuxScreen(
    viewModel: RexNuxViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    when {
        state.showSettings -> {
            RexNuxSettingsScreen(
                state = state,
                onBack = viewModel::closeSettings,
                onRemoveDistro = viewModel::removeDistro
            )
        }

        state.downloading -> {
            RexNuxInstallingScreen(
                state = state
            )
        }

        state.showDownloadScreen -> {
            RexNuxDownloadScreen(
                state = state,
                onBack = viewModel::closeDownloadScreen,
                onSelectDistro = viewModel::selectDistro,
                onInstall = viewModel::startInstall
            )
        }

        else -> {
            RexNuxHomeScreen(
                state = state,
                onDownload = viewModel::openDownloadScreen,
                onSettings = viewModel::openSettings,
                onOpenRexNux = viewModel::openRexNux
            )
        }
    }
}

/*
 * ------------------------------------------------------------
 * HOME
 * ------------------------------------------------------------
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RexNuxHomeScreen(
    state: RexNuxUiState,
    onDownload: () -> Unit,
    onSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "RexNux",
                        fontWeight = FontWeight.SemiBold
                    )
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
                error = state.error
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
private fun RexNuxNoDistroContent(
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
private fun RexNuxInstalledContent(
    modifier: Modifier = Modifier,
    distro: RexNuxDistro,
    error: String?
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
                        onClick = {
                            /*
                             * Runtime RexNux akan dipasang di sini.
                             *
                             * Untuk sekarang rootfs sudah bisa
                             * didownload dan diekstrak oleh installer.
                             */
                        },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RexNuxDownloadScreen(
    state: RexNuxUiState,
    onBack: () -> Unit,
    onSelectDistro: (RexNuxDistro) -> Unit,
    onInstall: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Download Distro")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
        ) {

            Text(
                text = "Choose your Linux",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "Pilih distro Linux untuk RexNux.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(
                    items = RexNuxDistro.entries
                ) { distro ->

                    val selected =
                        state.selectedDistro == distro

                    RexNuxDistroCard(
                        distro = distro,
                        selected = selected,
                        onClick = {
                            onSelectDistro(distro)
                        }
                    )
                }
            }

            if (!state.error.isNullOrBlank()) {
                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                RexNuxErrorMessage(
                    message = state.error
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                onClick = onInstall,
                enabled = state.selectedDistro != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.size(8.dp)
                )

                Text("Install Selected Distro")
            }
        }
    }
}

/*
 * ------------------------------------------------------------
 * DISTRO CARD
 * ------------------------------------------------------------
 */

@Composable
private fun RexNuxDistroCard(
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

/*
 * ------------------------------------------------------------
 * INSTALLING
 * ------------------------------------------------------------
 */

@Composable
private fun RexNuxInstallingScreen(
    state: RexNuxUiState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        CircularProgressIndicator()

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "Installing ${
                state.selectedDistro?.title ?: "RexNux"
            }",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = state.message.ifBlank {
                "Preparing..."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        LinearProgressIndicator(
            progress = {
                state.progress
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "${(state.progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/*
 * ------------------------------------------------------------
 * SETTINGS
 * ------------------------------------------------------------
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RexNuxSettingsScreen(
    state: RexNuxUiState,
    onBack: () -> Unit,
    onRemoveDistro: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("RexNux Settings")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                Text(
                    text = "Distro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (state.installedDistro == null) {

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Text(
                                text = "No distro installed",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text = "Install Alpine Linux atau Arch Linux " +
                                        "untuk menggunakan RexNux.",
                                color = MaterialTheme.colorScheme
                                    .onSurfaceVariant
                            )
                        }
                    }
                }

            } else {

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text =
                                        state.installedDistro.title,
                                    style =
                                        MaterialTheme.typography
                                            .titleLarge,
                                    fontWeight =
                                        FontWeight.SemiBold
                                )

                                Spacer(
                                    modifier = Modifier.height(4.dp)
                                )

                                Text(
                                    text = "Installed",
                                    color =
                                        MaterialTheme.colorScheme
                                            .primary
                                )
                            }

                            Icon(
                                imageVector =
                                    Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint =
                                    MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = onRemoveDistro,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null
                        )

                        Spacer(
                            modifier = Modifier.size(8.dp)
                        )

                        Text("Remove Distro")
                    }
                }
            }
        }
    }
}

/*
 * ------------------------------------------------------------
 * ERROR
 * ------------------------------------------------------------
 */

@Composable
private fun RexNuxErrorMessage(
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(
                modifier = Modifier.size(10.dp)
            )

            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
