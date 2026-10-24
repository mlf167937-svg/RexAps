@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun RexGitScreenLandscape(
    state: RexGitUiState,
    host: RexGitScreenHost,
    editor: RexGitEditorState?,
    viewModel: RexGitViewModel
) {
    val screen = screenIndex(state)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(
                hostState = host.snackbar,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = if (screen == 1) LandscapeRepoPaneWidth else 0.dp)
            )
        },
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) { padding ->

        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Crossfade(
                targetState = screen,
                animationSpec = tween(160)
            ) { target ->
                when (target) {
                    2 -> editor?.let {
                        EditorScreenLandscape(
                            state = state,
                            editor = it,
                            onBack = {
                                if (state.editorDirty) host.discardDialog.value = true
                                else viewModel.closeEditor()
                            },
                            onSave = viewModel::saveEditor
                        )
                    }

                    1 -> RepositoryScreenLandscape(
                        state = state,
                        host = host,
                        onBack = viewModel::backToRepositories,
                        onRefresh = viewModel::refreshStatus,
                        onRefreshHub = viewModel::refreshRepositoryHub,
                        onDownloadAsset = viewModel::downloadGithubAsset,
                        onOpen = viewModel::openFile,
                        onNavigate = { path ->
                            host.repoTab.value = 1
                            if (path == null) viewModel.openRoot()
                            else viewModel.openDirectory(path)
                        },
                        onPull = viewModel::pull
                    )

                    else -> HomeScreenLandscape(
                        state = state,
                        host = host,
                        onRefresh = viewModel::refresh,
                        onSelect = viewModel::selectRepository,
                        onClone = viewModel::cloneRepository,
                        onSwitch = viewModel::switchAccount,
                        onSearchGithub = viewModel::searchGithubRepositories
                    )
                }
            }
        }
    }
}
