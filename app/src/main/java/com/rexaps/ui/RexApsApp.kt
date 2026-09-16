package com.rexaps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class RexApp(
    val name: String,
    val description: String
)

@Composable
fun RexApsApp(
    onOpenRexFox: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val apps = listOf(
        RexApp("RexFox", "Browser cepat & ringan"),
        RexApp("RexNux", "Terminal & Linux"),
        RexApp("RexMusic", "Musik & playlist"),
        RexApp("RexTube", "Video & subscriptions"),
        RexApp("RexTok", "Short video & feed")
    )

    MaterialTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(Icons.Default.Home, contentDescription = "Home")
                        },
                        label = { Text("Home") }
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(Icons.Default.Person, contentDescription = "Profile")
                        },
                        label = { Text("Profile") }
                    )
                }
            }
        ) { padding ->

            if (selectedTab == 0) {
                HomeContent(
                    padding = padding,
                    apps = apps,
                    onOpenRexFox = onOpenRexFox
                )
            } else {
                ProfileContent(padding)
            }
        }
    }
}

@Composable
private fun HomeContent(
    padding: PaddingValues,
    apps: List<RexApp>,
    onOpenRexFox: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp)
    ) {

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "RexAps",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Semua aplikasimu, satu tempat.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Aplikasi",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(apps) { app ->
                AppCard(
                    app = app,
                    onClick = {
                        if (app.name == "RexFox") {
                            onOpenRexFox()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AppCard(
    app: RexApp,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.name.take(1),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Column {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = app.description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    padding: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Profile RexAps",
            style = MaterialTheme.typography.headlineSmall
        )
    }
}
