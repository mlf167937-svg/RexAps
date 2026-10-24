package com.rexaps.rexfox

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun RexFoxHome(
    state: BrowserUiState,
    onNavigate: (String) -> Unit,
    onTabs: () -> Unit,
    onBookmarks: () -> Unit,
    onHistory: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit
) { __RexFoxHomeContent(state, onNavigate, onTabs, onBookmarks, onHistory, onDownloads, onSettings) }
