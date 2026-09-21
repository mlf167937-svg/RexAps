package com.rexaps.rexchat

import android.app.Activity
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.rexaps.rexfox.rexPressable

@Composable
fun RexChatScreen(activity: Activity, onExit: () -> Unit) {
    val owner = activity as? ViewModelStoreOwner
        ?: error("RexChatScreen requires an Activity that implements ViewModelStoreOwner")
    val vm = ViewModelProvider(owner, RexChatViewModel.Factory(activity))[RexChatViewModel::class.java]
    val colors = MaterialTheme.colorScheme

    var showMenu by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) {
        vm.onFilesPicked(it)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.onPermissionsResult(it.isNotEmpty() && it.values.all { ok -> ok })
    }

    DisposableEffect(vm) {
        vm.onPickFiles = { filePicker.launch("*/*") }
        vm.onNeedPermissions = { permissionLauncher.launch(it) }
        onDispose {
            vm.onPickFiles = null
            vm.onNeedPermissions = null
            vm.flush()
        }
    }

    BackHandler {
        if (vm.webView.canGoBack()) vm.webView.goBack() else onExit()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        /* ------------------------------ HEADER ------------------------------ */
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExit) { Icon(Icons.Default.ArrowBack, "Kembali") }

            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Chat, null, tint = colors.onPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text("RexChat", style = MaterialTheme.typography.titleMedium)
                Text(
                    "WhatsApp Web",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }

            IconButton(onClick = { vm.reload() }) { Icon(Icons.Default.Refresh, "Muat ulang") }

            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "Menu") }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Hapus sesi (logout)") },
                        leadingIcon = { Icon(Icons.Default.Logout, null) },
                        onClick = {
                            showMenu = false
                            confirmClear = true
                        }
                    )
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(2.dp)) {
            if (vm.loading) {
                LinearProgressIndicator(
                    progress = { vm.progress / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = colors.tertiary,
                    trackColor = Color.Transparent
                )
            }
        }

        /* ------------------------------ CONTENT ----------------------------- */
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = {
                    // Cegah crash "child already has a parent" saat layar dibuka ulang.
                    (vm.webView.parent as? ViewGroup)?.removeView(vm.webView)
                    vm.webView
                },
                modifier = Modifier.fillMaxSize()
            )

            vm.error?.let { message ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(colors.background)
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .background(colors.surfaceVariant, CircleShape)
                            .border(1.dp, colors.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.WifiOff, null, tint = colors.tertiary, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        message,
                        textAlign = TextAlign.Center,
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(20.dp))
                    Box(
                        Modifier
                            .rexPressable { vm.reload() }
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary)))
                            .padding(horizontal = 28.dp, vertical = 12.dp)
                    ) {
                        Text("Coba lagi", color = colors.onPrimary, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Hapus sesi?") },
            text = {
                Text("Kamu akan logout dari WhatsApp Web di aplikasi ini dan perlu scan QR atau kode pairing lagi.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    vm.clearSession()
                }) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Batal") }
            }
        )
    }
}
