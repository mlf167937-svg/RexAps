package com.rexaps.rexchat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun WaScreen(
    c: WaController,
    title: String,
    onExit: () -> Unit,
    onDeleteSession: () -> Unit,
    onOpenWhatsApp: () -> Unit
) {
    val ctx = LocalContext.current
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
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExit) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                StatusChip(c.login)
            }
            IconButton(onClick = { c.reload() }) { Icon(Icons.Default.Refresh, "Reload") }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Menu") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Link with phone number") },
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        onClick = { menu = false; c.askPhone = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Show QR for another phone") },
                        leadingIcon = { Icon(Icons.Default.QrCode2, null) },
                        onClick = { menu = false; c.qrDialogOpen = true }
                    )
                    DropdownMenuItem(
                        text = { Text(if (c.desktopLayout) "Mobile layout" else "Desktop layout") },
                        leadingIcon = { Icon(Icons.Default.DesktopWindows, null) },
                        onClick = { menu = false; c.toggleDesktopLayout() }
                    )
                    DropdownMenuItem(
                        text = { Text("Open WhatsApp app") },
                        leadingIcon = { Icon(Icons.Default.Chat, null) },
                        onClick = { menu = false; onOpenWhatsApp() }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete session") },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { menu = false; confirmDelete = true }
                    )
                }
            }
        }

        // ---------- progress ----------
        Box(Modifier.fillMaxWidth().height(2.dp)) {
            if (c.loading) {
                LinearProgressIndicator(
                    progress = { c.progress.coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxSize(),
                    trackColor = Color.Transparent
                )
            }
        }

        // ---------- web ----------
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = {
                    (c.webView.parent as? ViewGroup)?.removeView(c.webView)
                    c.webView
                },
                modifier = Modifier.fillMaxSize()
            )

            c.error?.let { msg ->
                Column(
                    Modifier.fillMaxSize().background(colors.background).padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.WifiOff, null, Modifier.size(48.dp), tint = colors.primary)
                    Spacer(Modifier.height(16.dp))
                    Text(msg, textAlign = TextAlign.Center, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { c.reload() }) { Text("Try again") }
                }
            }

            // Link banner while logged out
            if (c.error == null && c.login == Login.QR) {
                Card(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Link this session", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Same phone? Use a pairing code. Another phone? Show the QR.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { c.askPhone = true }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Phone, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Phone no.")
                            }
                            OutlinedButton(onClick = { c.qrDialogOpen = true }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.QrCode2, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("QR")
                            }
                        }
                    }
                }
            }
        }
    }

    // ---------- dialogs ----------

    if (c.askPhone) {
        var number by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { c.askPhone = false },
            title = { Text("Link with phone number") },
            text = {
                Column {
                    Text(
                        "Enter the number of the WhatsApp account you want to link, with country code.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it },
                        singleLine = true,
                        label = { Text("Phone number") },
                        placeholder = { Text("+62 812 3456 7890") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { c.askPhone = false; c.startPairing(number) }) { Text("Get code") }
            },
            dismissButton = { TextButton(onClick = { c.askPhone = false }) { Text("Cancel") } }
        )
    }

    when (val p = c.pairing) {
        Pairing.Idle -> Unit
        Pairing.Working -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Requesting code…") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.width(16.dp))
                    Text("Talking to WhatsApp Web")
                }
            },
            confirmButton = { TextButton(onClick = { c.cancelPairing() }) { Text("Cancel") } }
        )
        is Pairing.Code -> AlertDialog(
            onDismissRequest = { c.cancelPairing() },
            title = { Text("Your pairing code") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        p.code,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 3.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "In WhatsApp: Settings → Linked devices → Link a device → " +
                                "Link with phone number instead, then enter this code.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("code", p.code))
                        }) {
                            Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp)); Text("Copy")
                        }
                        Button(onClick = onOpenWhatsApp) { Text("Open WhatsApp") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { c.cancelPairing() }) { Text("Done") } }
        )
        is Pairing.Failed -> AlertDialog(
            onDismissRequest = { c.cancelPairing() },
            title = { Text("Couldn't get code") },
            text = { Text(p.message) },
            confirmButton = { TextButton(onClick = { c.cancelPairing() }) { Text("OK") } }
        )
    }

    if (c.qrDialogOpen) {
        AlertDialog(
            onDismissRequest = { c.qrDialogOpen = false },
            title = { Text("Scan with another phone") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier.size(260.dp).clip(RoundedCornerShape(12.dp)).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        val bmp = c.qrBitmap
                        if (bmp != null) {
                            Image(bmp.asImageBitmap(), "QR code", Modifier.fillMaxSize())
                        } else {
                            CircularProgressIndicator()
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "On the other phone: WhatsApp → Linked devices → Link a device. " +
                                "The code refreshes automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = { TextButton(onClick = { c.qrDialogOpen = false }) { Text("Close") } }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete session?") },
            text = { Text("This removes the saved login and all WebView data for this session.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDeleteSession() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StatusChip(login: Login) {
    val (text, color) = when (login) {
        Login.LOADING -> "Connecting…" to MaterialTheme.colorScheme.onSurfaceVariant
        Login.QR -> "Not linked" to MaterialTheme.colorScheme.error
        Login.CODE -> "Pairing…" to MaterialTheme.colorScheme.tertiary
        Login.CHATS -> "Linked" to MaterialTheme.colorScheme.primary
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}