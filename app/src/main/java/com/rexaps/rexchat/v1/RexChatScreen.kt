package com.rexaps.rexchat.v1

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

private enum class RexTab { CHATS, PLUGINS, ACCOUNT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexChatScreen(
    sessionName: String,
    sessionId: Int,
    initialMethod: String,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    val runtime by RexChatRuntime.stateFor(sessionId).collectAsState()
    var tab by remember { mutableStateOf(RexTab.CHATS) }
    var phone by remember { mutableStateOf("") }
    var showPairing by remember { mutableStateOf(initialMethod.isNotBlank()) }
    var showDelete by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == RexTab.CHATS,
                    onClick = { tab = RexTab.CHATS },
                    icon = { Icon(Icons.Default.ChatBubbleOutline, null) },
                    label = { Text("Chats") }
                )
                NavigationBarItem(
                    selected = tab == RexTab.PLUGINS,
                    onClick = { tab = RexTab.PLUGINS },
                    icon = { Icon(Icons.Default.Extension, null) },
                    label = { Text("Plugins") }
                )
                NavigationBarItem(
                    selected = tab == RexTab.ACCOUNT,
                    onClick = { tab = RexTab.ACCOUNT },
                    icon = { Icon(Icons.Default.Smartphone, null) },
                    label = { Text("Akun") }
                )
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") }
                Column(Modifier.weight(1f)) {
                    Text("RexChat", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(sessionName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = CircleShape, color = if (runtime.connected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(if (runtime.connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))
                        Spacer(Modifier.width(6.dp))
                        Text(if (runtime.connected) "Online" else "Offline", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            when (tab) {
                RexTab.CHATS -> ChatHome(
                    runtime = runtime,
                    onPair = { showPairing = true },
                    onSend = { chatId, message -> RexChatRuntime.sendText(sessionId, chatId, message) }
                )
                RexTab.PLUGINS -> PluginHome()
                RexTab.ACCOUNT -> AccountHome(
                    runtime = runtime,
                    phone = phone,
                    onPhoneChange = { phone = it },
                    onPair = { RexChatRuntime.requestPairingCode(sessionId, phone.filter(Char::isDigit)) },
                    onRefresh = { RexChatRuntime.startFromUi(sessionId) },
                    onDelete = { showDelete = true },
                    onShowPair = { showPairing = true }
                )
            }
        }
    }

    if (showPairing) {
        AlertDialog(
            onDismissRequest = { showPairing = false },
            title = { Text("Hubungkan WhatsApp") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(runtime.detail)
                    if (runtime.status != RexChatRuntime.Status.READY) {
                        Text("QR dan pairing code belum dapat dibuat sampai runtime Node.js dan dependency Baileys tersedia.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (runtime.qr != null) QrPreview(runtime.qr!!)
                    runtime.pairingCode?.let { Text(it, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                    if (runtime.qr == null && runtime.pairingCode == null) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Nomor WhatsApp") },
                            placeholder = { Text("62812…") },
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = runtime.status == RexChatRuntime.Status.READY,
                    onClick = {
                        if (phone.isNotBlank()) RexChatRuntime.requestPairingCode(sessionId, phone.filter(Char::isDigit))
                        else RexChatRuntime.startFromUi(sessionId)
                    }
                ) { Text(if (phone.isBlank()) "Muat ulang QR" else "Minta kode") }
            },
            dismissButton = { TextButton(onClick = { showPairing = false }) { Text("Tutup") } }
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Hapus akun ini?") },
            text = { Text("Data session lokal akun ini akan dihapus dari RexChat.") },
            confirmButton = { TextButton(onClick = onDelete) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Batal") } }
        )
    }
}

@Composable
private fun ChatHome(
    runtime: RexChatRuntime.State,
    onPair: () -> Unit,
    onSend: (String, String) -> Unit
) {
    var chatId by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        RuntimeBanner(runtime.detail, runtime.connected)
        Spacer(Modifier.height(12.dp))

        if (!runtime.connected) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(34.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Hubungkan WhatsApp", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Hubungkan akun untuk menerima dan mengirim pesan.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onPair) {
                        Icon(Icons.Default.Link, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Hubungkan akun")
                    }
                }
            }
        } else {
            Text("Pesan terbaru", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            if (runtime.messages.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "Belum ada pesan. Pesan masuk akan tampil di sini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(runtime.messages.takeLast(100)) { message ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { chatId = message.chatId },
                            colors = CardDefaults.cardColors(
                                containerColor = if (message.fromMe) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(
                                    if (message.fromMe) "Kamu · ${message.chatId}" else message.chatId,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(message.text, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Ketuk untuk membalas",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = chatId,
                onValueChange = { chatId = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nomor WhatsApp atau ID chat") },
                placeholder = { Text("62812… atau 12345-678@g.us") },
                singleLine = true
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Pesan") },
                    singleLine = false,
                    maxLines = 3
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = chatId.isNotBlank() && draft.isNotBlank(),
                    onClick = {
                        onSend(chatId, draft)
                        draft = ""
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Kirim")
                }
            }
            Text(
                "Tip: ketuk pesan untuk mengisi ID chat tujuan secara otomatis.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun RuntimeBanner(detail: String, connected: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = if (connected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (connected) Icons.Default.Link else Icons.Default.Info, null)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(if (connected) "WhatsApp tersambung" else "Runtime belum siap", fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PluginHome() {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Plugin Studio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Buat automasi JavaScript sendiri. Plugin dijalankan lokal di perangkat.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { PluginCard("menu.js", "Command handler", "Contoh plugin untuk /menu dan .menu", true) }
        item { PluginCard("RichHtml", "Library bersama", "Bangun pesan kaya melalui helper plugin", true) }
        item { PluginCard("Plugin API", "Fondasi V1", "API plugin sedang disiapkan; eksekusi plugin belum aktif sebelum runtime tersedia.", false) }
        item {
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Buat plugin (segera)")
            }
        }
    }
}

@Composable
private fun PluginCard(name: String, category: String, description: String, ready: Boolean) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.tertiaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Code, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold)
                Text(category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (ready) "Draft" else "Soon", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccountHome(
    runtime: RexChatRuntime.State,
    phone: String,
    onPhoneChange: (String) -> Unit,
    onPair: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
    onShowPair: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Akun & koneksi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Session disimpan lokal di perangkat.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { RuntimeBanner(runtime.detail, runtime.connected) }
        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tautkan perangkat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(value = phone, onValueChange = onPhoneChange, modifier = Modifier.fillMaxWidth(), label = { Text("Nomor telepon") }, placeholder = { Text("62812…") }, singleLine = true)
                    Button(onClick = onPair, modifier = Modifier.fillMaxWidth(), enabled = runtime.status == RexChatRuntime.Status.READY && phone.filter(Char::isDigit).length >= 8) {
                        Icon(Icons.Default.Send, null); Spacer(Modifier.width(8.dp)); Text("Minta pairing code")
                    }
                    OutlinedButton(onClick = onShowPair, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.QrCode2, null); Spacer(Modifier.width(8.dp)); Text("Tampilkan QR / metode lain")
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Periksa runtime")
            }
        }
        item {
            TextButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, null); Spacer(Modifier.width(8.dp)); Text("Hapus session")
            }
        }
    }
}

@Composable
private fun QrPreview(value: String) {
    val bitmap = remember(value) {
        runCatching {
            val matrix = QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 640, 640)
            Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888).apply {
                for (x in 0 until 640) for (y in 0 until 640) setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }.getOrNull()
    }
    bitmap?.let {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Image(bitmap = it.asImageBitmap(), contentDescription = "QR WhatsApp", modifier = Modifier.size(220.dp))
        }
    }
}
