package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RexMusicScreen(
    onBack: () -> Unit = {},
    vm: RexMusicViewModel = viewModel()
) {
    RexMusicTheme {
        RexMusicContent(onBack, vm)
    }
}
