package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*

@Composable
fun RexMusicSyncScreen(onBack: () -> Unit) {
    RexMusicSyncScreenContent(onBack = onBack)
}
