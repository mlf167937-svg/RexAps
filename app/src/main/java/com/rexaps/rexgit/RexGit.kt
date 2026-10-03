package com.rexaps.rexgit

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RexGit() {
    val vm: RexGitViewModel = viewModel()

    RexGitScreen(
        viewModel = vm
    )
}
