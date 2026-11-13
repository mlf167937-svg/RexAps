package com.rexaps.rexnux

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexNuxScreen(
    onExit: () -> Unit = {},
    viewModel: RexNuxViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var distroToDelete by remember { mutableStateOf<RexNuxDistro?>(null) }

    when {
        state.showTerminal && state.activeDistro != null -> {
            RexNuxTerminalScreen(
                distro = state.activeDistro!!,
                onBack = viewModel::closeTerminal
            )
        }
        state.downloading -> {
            RexNuxInstallingDialog(state = state)
        }
        else -> {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text("RexNux", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("ARM64 Linux Subsystem (PRoot)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onExit) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                            }
                        },
                        actions = {
                            IconButton(onClick = viewModel::refreshState) {
                                Icon(Icons.Default.Terminal, contentDescription = "Refresh")
                            }
                        }
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "Multi-Distro Linux ARM64",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "RexNux menjalankan distribusi Linux sungguhan menggunakan PRoot di userland Android. Anda dapat menginstal beberapa distro sekaligus, menjalankan shell interaktif, dan memasang paket menggunakan pacman, apk, atau apt.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (!state.error.isNullOrBlank()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.width(10.dp))
                                    Text(state.error!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            "Daftar Distribusi Linux",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    items(RexNuxDistro.entries) { distro ->
                        val isInstalled = state.installedDistros.contains(distro)
                        val sizeText = state.distroSizes[distro] ?: ""

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isInstalled) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isInstalled) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Terminal,
                                            contentDescription = null,
                                            tint = if (isInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(distro.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(distro.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isInstalled) {
                                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "Terpasang • $sizeText",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        } else {
                                            Text(
                                                "Belum Terpasang",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (isInstalled) {
                                            IconButton(
                                                onClick = { distroToDelete = distro }
                                            ) {
                                                Icon(Icons.Default.Delete, "Hapus", tint = MaterialTheme.colorScheme.error)
                                            }
                                            Button(
                                                onClick = { viewModel.openTerminal(distro) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Launch")
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    viewModel.selectDistro(distro)
                                                    viewModel.startInstall()
                                                }
                                            ) {
                                                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Install")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("Informasi Arsitektur", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                Spacer(Modifier.height(4.dp))
                                Text("Root Path: /data/data/com.rexaps/rexnux/<distro>/", style = MaterialTheme.typography.bodySmall)
                                Text("User Home: /files/home (HOME=/files/home, USER=root)", style = MaterialTheme.typography.bodySmall)
                                Text("ABI Target: arm64-v8a • PRoot: Userland Namespace Isolation", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            distroToDelete?.let { distro ->
                AlertDialog(
                    onDismissRequest = { distroToDelete = null },
                    title = { Text("Hapus ${distro.title}?") },
                    text = {
                        Text("Root filesystem dan file di dalam distro ini akan dihapus secara permanen. Distro lain yang terpasang tidak akan terpengaruh.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.removeDistro(distro)
                                distroToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Hapus")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { distroToDelete = null }) {
                            Text("Batal")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RexNuxInstallingDialog(state: RexNuxUiState) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text("Memasang Distribusi Linux", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(state.message, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${(state.progress * 100).toInt()}% selesai",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {}
    )
}
