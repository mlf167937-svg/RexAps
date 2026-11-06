package com.rexaps.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

private val rexShortcutPalette = listOf(
    0xFF3977E8.toInt(), 0xFF7A55D8.toInt(), 0xFF13A889.toInt(),
    0xFFE15B72.toInt(), 0xFFDB9639.toInt(), 0xFF2E9BC5.toInt(),
    0xFF3D4658.toInt(), 0xFFAD5EB7.toInt()
)

@Composable
internal fun RexShortcutSettingsCard(context: Context) {
    val colors = MaterialTheme.colorScheme
    val enabledModules = remember {
        RexAppRegistry.modules.filter { it.available && RexRoute.fromModuleId(it.id) != null }
    }
    val initialModule = remember { enabledModules.firstOrNull { it.id == "rexgit" } ?: enabledModules.firstOrNull() }
    var selectedModuleId by rememberSaveable { mutableStateOf(initialModule?.id.orEmpty()) }
    val selectedModule = enabledModules.firstOrNull { it.id == selectedModuleId } ?: initialModule
    var moduleMenuExpanded by remember { mutableStateOf(false) }
    var shortcutLabel by rememberSaveable { mutableStateOf(selectedModule?.name ?: "RexGit") }
    var glyph by rememberSaveable { mutableStateOf(selectedModule?.let(::defaultShortcutGlyph) ?: "RG") }
    var iconColor by rememberSaveable { mutableIntStateOf(rexShortcutPalette.first()) }
    var customImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var customImageMode by rememberSaveable { mutableStateOf(false) }
    var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var statusIsSuccess by rememberSaveable { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            customImageUri = uri.toString()
            customImageMode = true
            statusMessage = "Gambar ikon dipilih. Preview akan dipotong ke bentuk persegi."
            statusIsSuccess = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceVariant.copy(alpha = .58f),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .65f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(colors.primary.copy(alpha = .14f)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Default.AddToHomeScreen,
                        contentDescription = null,
                        tint = colors.primary
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Tambah shortcut", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Buka modul langsung dari home screen HP.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            Text(
                "Pilih modul yang aktif di RexAps. Shortcut dibuat melalui konfirmasi launcher Android.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )

            Box {
                OutlinedButton(onClick = { moduleMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("${selectedModule?.name ?: "Pilih modul"}  ▾")
                }
                DropdownMenu(
                    expanded = moduleMenuExpanded,
                    onDismissRequest = { moduleMenuExpanded = false }
                ) {
                    enabledModules.forEach { module ->
                        DropdownMenuItem(
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(module.name, fontWeight = FontWeight.SemiBold)
                                    Text(module.description, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                selectedModuleId = module.id
                                shortcutLabel = module.name
                                glyph = defaultShortcutGlyph(module)
                                statusMessage = null
                                moduleMenuExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = shortcutLabel,
                onValueChange = { shortcutLabel = it.take(24) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Nama shortcut") },
                supportingText = { Text("Maksimal 24 karakter") }
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(72.dp).clip(RoundedCornerShape(21.dp)).background(
                        Brush.linearGradient(listOf(Color(iconColor), Color(iconColor).copy(alpha = .72f)))
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    if (customImageMode && customImageUri != null) {
                        AsyncImage(
                            model = Uri.parse(customImageUri),
                            contentDescription = "Preview ikon shortcut",
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            glyph.ifBlank { selectedModule?.let(::defaultShortcutGlyph) ?: "RX" }.take(3).uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Preview ikon", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (customImageMode && customImageUri != null) "Gambar pilihan · crop otomatis"
                        else "Ikon manual · warna dan simbol bisa diatur",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !customImageMode,
                    onClick = { customImageMode = false },
                    label = { Text("Manual") },
                    leadingIcon = { androidx.compose.material3.Icon(Icons.Default.Palette, null, Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = customImageMode,
                    onClick = { customImageMode = true },
                    label = { Text("Gambar custom") },
                    leadingIcon = { androidx.compose.material3.Icon(Icons.Default.Image, null, Modifier.size(16.dp)) }
                )
            }

            if (customImageMode) {
                OutlinedButton(onClick = { imagePicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (customImageUri == null) "Pilih gambar dari perangkat" else "Ganti gambar ikon")
                }
                Text(
                    "Gambar diproses menjadi ikon launcher persegi. File asli tidak diubah.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            } else {
                OutlinedTextField(
                    value = glyph,
                    onValueChange = { glyph = it.take(3) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Simbol ikon") },
                    supportingText = { Text("1–3 huruf, emoji, atau simbol") }
                )
                Text("Warna ikon", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    rexShortcutPalette.forEach { colorValue ->
                        val chosen = colorValue == iconColor
                        Box(
                            Modifier
                                .size(31.dp)
                                .clip(CircleShape)
                                .background(Color(colorValue))
                                .then(
                                    if (chosen) Modifier.border(3.dp, colors.onSurface, CircleShape)
                                    else Modifier.border(1.dp, colors.outlineVariant, CircleShape)
                                )
                                .clickable { iconColor = colorValue }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val module = selectedModule
                    if (module == null) {
                        statusMessage = "Tidak ada modul yang tersedia untuk dibuatkan shortcut."
                        statusIsSuccess = false
                    } else {
                        runCatching {
                            val imageUri = if (customImageMode) customImageUri?.let { value -> Uri.parse(value) } else null
                            val bitmap = createRexShortcutBitmap(
                                context = context,
                                module = module,
                                accentColor = iconColor,
                                glyph = glyph.ifBlank { defaultShortcutGlyph(module) },
                                customImage = imageUri
                            )
                            val iconVariant = if (customImageMode) {
                                "image:${customImageUri.orEmpty()}"
                            } else {
                                "manual:${glyph.trim()}:$iconColor"
                            }
                            requestRexModuleShortcut(
                                context = context,
                                module = module,
                                label = shortcutLabel,
                                bitmap = bitmap,
                                iconVariant = iconVariant
                            )
                        }.onSuccess { accepted ->
                            statusIsSuccess = accepted
                            statusMessage = if (accepted) {
                                "Permintaan dikirim. Konfirmasi penambahan ikon pada launcher Android."
                            } else {
                                "Launcher perangkat tidak mendukung pinned shortcut."
                            }
                        }.onFailure { error ->
                            statusIsSuccess = false
                            statusMessage = error.message ?: "Shortcut gagal dibuat."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedModule != null && shortcutLabel.isNotBlank() && (!customImageMode || customImageUri != null)
            ) {
                androidx.compose.material3.Icon(Icons.Default.AddToHomeScreen, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Tambah ke Home Screen")
            }

            statusMessage?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (statusIsSuccess) colors.primary else colors.error
                )
            }
            Text(
                "${enabledModules.size} modul aktif tersedia untuk shortcut. Ikon ditambahkan oleh launcher, jadi hasil akhir dapat mengikuti kebijakan perangkat.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}
