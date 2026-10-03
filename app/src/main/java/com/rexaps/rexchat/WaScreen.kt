package com.rexaps.rexchat

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun WaScreen(
    c: WaController,
    title: String,
    onExit: () -> Unit,
    onDeleteSession: () -> Unit,
    onOpenWhatsApp: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    BackHandler { if (c.canGoBack()) c.goBack() else onExit() }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {

        // ---------- header ----------
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExit) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }

            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                StatusChip(c.login)
            }

            IconButton(onClick = { c.reload() }) {
                Icon(Icons.Default.Refresh, "Reload")
            }

            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, "Menu")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (c.desktopLayout) "Tampilan normal" else "Tampilan desktop") },
                        leadingIcon = { Icon(Icons.Default.DesktopWindows, null) },
                        onClick = { menu = false; c.toggleDesktopLayout() }
                    )
                    DropdownMenuItem(
                        text = { Text("Buka aplikasi WhatsApp") },
                        leadingIcon = { Icon(Icons.Default.Chat, null) },
                        onClick = { menu = false; onOpenWhatsApp() }
                    )
                    DropdownMenuItem(
                        text = { Text("Tampilkan petunjuk") },
                        leadingIcon = { Icon(Icons.Default.Chat, null) },
                        onClick = { menu = false; c.hintDismissed = false }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Hapus session") },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { menu = false; confirmDelete = true }
                    )
                }
            }
        }

        // ---------- progress ----------
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
        ) {
            if (c.loading) {
                LinearProgressIndicator(
                    progress = { c.progress.coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxSize(),
                    trackColor = Color.Transparent
                )
            }
        }

        // ---------- web ----------
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = {
                    (c.webView.parent as? ViewGroup)?.removeView(c.webView)
                    c.webView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Petunjuk saat belum login
            if (c.error == null && c.login != Login.CHATS && !c.hintDismissed) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(10.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Cara menautkan",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { c.hintDismissed = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, "Tutup", Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "HP yang sama: pada halaman di atas ketuk \"Link with phone number\", " +
                                    "masukkan nomor, lalu masukkan kode 8 karakter di WhatsApp " +
                                    "(Perangkat tertaut > Tautkan perangkat > Tautkan dengan nomor telepon).",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "HP lain: scan QR yang tampil di halaman.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onOpenWhatsApp, modifier = Modifier.weight(1f)) {
                                Text("Buka WhatsApp")
                            }
                            OutlinedButton(
                                onClick = { c.toggleDesktopLayout() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (c.desktopLayout) "Normal" else "Desktop")
                            }
                        }
                    }
                }
            }

            // Error
            c.error?.let { msg ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(colors.background)
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.WifiOff, null, Modifier.size(48.dp), tint = colors.primary)
                    Spacer(Modifier.height(16.dp))
                    Text(msg, textAlign = TextAlign.Center, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { c.reload() }) { Text("Coba lagi") }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus session?") },
            text = { Text("Login tersimpan dan semua data WebView session ini akan dihapus.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDeleteSession() }) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun StatusChip(login: Login) {
    val (text, color) = when (login) {
        Login.LOADING -> "Menghubungkan…" to MaterialTheme.colorScheme.onSurfaceVariant
        Login.LOGGED_OUT -> "Belum tertaut" to MaterialTheme.colorScheme.error
        Login.CHATS -> "Tertaut" to MaterialTheme.colorScheme.primary
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}