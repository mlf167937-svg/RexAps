package com.rexaps.rexnux

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RexNuxDownloadScreen(
    state: RexNuxUiState,
    onBack: () -> Unit,
    onSelectDistro: (RexNuxDistro) -> Unit,
    onInstall: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.93f),
        topBar = {
            TopAppBar(
                title = {
                    Text("Download Distro")
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
        ) {

            Text(
                text = "Choose your Linux",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "Pilih distro Linux untuk RexNux.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(
                    items = RexNuxDistro.entries
                ) { distro ->

                    val selected =
                        state.selectedDistro == distro

                    RexNuxDistroCard(
                        distro = distro,
                        selected = selected,
                        onClick = {
                            onSelectDistro(distro)
                        }
                    )
                }
            }

            if (!state.error.isNullOrBlank()) {
                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                RexNuxErrorMessage(
                    message = state.error
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                onClick = onInstall,
                enabled = state.selectedDistro != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.size(8.dp)
                )

                Text("Install Selected Distro")
            }
        }
    }
}

/*
 * ------------------------------------------------------------
 * DISTRO CARD
 * ------------------------------------------------------------
 */
