package com.rexaps.rexnux

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
