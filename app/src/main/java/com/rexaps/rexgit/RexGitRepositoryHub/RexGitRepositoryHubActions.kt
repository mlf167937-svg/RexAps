@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.rexaps.rexgit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun RexGitActionsPanel(
    state: RexGitUiState,
    onRefresh: () -> Unit,
    onDownload: (String, String) -> Unit,
    onChooseAccount: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RexGitActionsHeaderAndStatus(state, onRefresh, onChooseAccount)
        RexGitActionsRunsAndArtifacts(state, onDownload)
        RexGitActionsWorkflowList(state)
    }
}
