package com.rexaps.rexnux

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RexNuxSettingsScreen(
    state: RexNuxUiState,
    onBack: () -> Unit,
    onRemoveDistro: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.93f),
        topBar = {
            TopAppBar(
                title = {
                    Text("RexNux Settings")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                Text(
                    text = "Distro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (state.installedDistro == null) {

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Text(
                                text = "No distro installed",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text = "Install Alpine Linux atau Arch Linux " +
                                        "untuk menggunakan RexNux.",
                                color = MaterialTheme.colorScheme
                                    .onSurfaceVariant
                            )
                        }
                    }
                }

            } else {

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text =
                                        state.installedDistro.title,
                                    style =
                                        MaterialTheme.typography
                                            .titleLarge,
                                    fontWeight =
                                        FontWeight.SemiBold
                                )

                                Spacer(
                                    modifier = Modifier.height(4.dp)
                                )

                                Text(
                                    text = "Installed",
                                    color =
                                        MaterialTheme.colorScheme
                                            .primary
                                )
                            }

                            Icon(
                                imageVector =
                                    Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint =
                                    MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = onRemoveDistro,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null
                        )

                        Spacer(
                            modifier = Modifier.size(8.dp)
                        )

                        Text("Remove Distro")
                    }
                }
            }
        }
    }
}

/*
 * ------------------------------------------------------------
 * ERROR
 * ------------------------------------------------------------
 */
