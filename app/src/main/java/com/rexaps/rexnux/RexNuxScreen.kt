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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    onExit: () -> Unit = {},
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

        state.showTerminal -> {
            RexNuxTerminalScreen(
                onBack = viewModel::closeRexNux
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
                onExit = onExit,
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
internal fun RexNuxHomeScreen(
    state: RexNuxUiState,
    onExit: () -> Unit,
    onDownload: () -> Unit,
    onSettings: () -> Unit,
    onOpenRexNux: () -> Unit
) {
        return __splitImpl_RexNuxHomeScreen(state, onExit, onDownload, onSettings, onOpenRexNux)
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
        return __splitImpl_RexNuxNoDistroContent(modifier, onDownload, error)
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
        return __splitImpl_RexNuxInstalledContent(modifier, distro, error, onOpen)
    }

/*
 * ------------------------------------------------------------
 * DOWNLOAD DISTRO
 * ------------------------------------------------------------
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RexNuxDownloadScreen(
    state: RexNuxUiState,
    onBack: () -> Unit,
    onSelectDistro: (RexNuxDistro) -> Unit,
    onInstall: () -> Unit
) {
        return __splitImpl_RexNuxDownloadScreen(state, onBack, onSelectDistro, onInstall)
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
internal fun RexNuxSettingsScreen(
    state: RexNuxUiState,
    onBack: () -> Unit,
    onRemoveDistro: () -> Unit
) {
        return __splitImpl_RexNuxSettingsScreen(state, onBack, onRemoveDistro)
    }

/*
 * ------------------------------------------------------------
 * ERROR
 * ------------------------------------------------------------
 */

@Composable
internal fun RexNuxErrorMessage(
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
