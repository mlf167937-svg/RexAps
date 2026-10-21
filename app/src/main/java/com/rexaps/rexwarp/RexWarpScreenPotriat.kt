package com.rexaps.rexwarp

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RexWarpScreenPortrait(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
    navItems: List<Dest>,
    navSelected: Dest,
    onNavigate: (Dest) -> Unit,
    snackbarHostState: SnackbarHostState,
    content: @Composable (Boolean) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val roomy = maxWidth >= 840.dp
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        if (showBack) IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.Transparent) {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            selected = navSelected == item,
                            onClick = { onNavigate(item) },
                            icon = { item.icon?.let { Icon(it, contentDescription = null) } },
                            label = { Text(item.navLabel, maxLines = 1) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.TopCenter
            ) { content(roomy) }
        }
    }
}
