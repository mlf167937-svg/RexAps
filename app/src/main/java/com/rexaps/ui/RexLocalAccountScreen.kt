package com.rexaps.ui

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

private data class RexLocalAccount(
    val name: String = "Rex User",
    val username: String = "@rexuser",
    val bio: String = "Halo! Aku menggunakan RexAps.",
    val bannerPath: String = "",
    val picturePath: String = ""
)

@Composable
fun RexLocalAccountScreen(padding: PaddingValues) {
    val context = LocalContext.current
    val root = remember {
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RexAps/.config/account")
    }
    val config = remember {
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RexAps/.config/account.json")
    }
    var account by remember { mutableStateOf(RexLocalAccount()) }
    var name by remember { mutableStateOf(account.name) }
    var username by remember { mutableStateOf(account.username) }
    var bio by remember { mutableStateOf(account.bio) }
    var bannerPath by remember { mutableStateOf("") }
    var picturePath by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Profil tersimpan secara lokal di perangkat ini.") }
    var pickBanner by remember { mutableStateOf(false) }

    fun loadAccount() {
        try {
            if (config.exists()) {
                val j = JSONObject(config.readText())
                account = RexLocalAccount(
                    j.optString("name", "Rex User"),
                    j.optString("username", "@rexuser"),
                    j.optString("bio", ""),
                    j.optString("banner", ""),
                    j.optString("profilePicture", "")
                )
                name = account.name
                username = account.username
                bio = account.bio
                bannerPath = account.bannerPath
                picturePath = account.picturePath
            }
        } catch (e: Exception) {
            status = "Gagal membaca account.json: ${e.message ?: "format tidak valid"}"
        }
    }
    LaunchedEffect(Unit) { loadAccount() }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                root.mkdirs()
                val isBanner = pickBanner
                val target = File(root, if (isBanner) "banner.png" else "profile-pictures.png")
                context.contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "File gambar tidak bisa dibuka" }.copyTo(FileOutputStream(target))
                }
                if (isBanner) bannerPath = target.absolutePath else picturePath = target.absolutePath
                status = "Gambar disimpan: ${target.name}. Tekan Simpan profil untuk memperbarui konfigurasi."
            } catch (e: Exception) {
                status = "Tidak dapat menyimpan gambar. Periksa izin penyimpanan: ${e.message ?: "akses ditolak"}"
            }
        }
    }

    val bannerBitmap = remember(bannerPath) {
        try { if (bannerPath.isNotBlank()) BitmapFactory.decodeFile(bannerPath) else null } catch (_: Exception) { null }
    }
    val avatarBitmap = remember(picturePath) {
        try { if (picturePath.isNotBlank()) BitmapFactory.decodeFile(picturePath) else null } catch (_: Exception) { null }
    }
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Akun lokal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Profil pribadi tanpa login atau server. Data hanya disimpan di perangkat.", color = colors.onSurfaceVariant)

        Box(
            modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(24.dp))
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(colors.primary, colors.tertiary)))
                .clickable { pickBanner = true; imagePicker.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (bannerBitmap != null) {
                Image(bannerBitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, null, tint = colors.onPrimary, modifier = Modifier.size(30.dp))
                    Text("Tambah banner akun", color = colors.onPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
            Surface(Modifier.align(Alignment.BottomEnd).padding(10.dp), color = colors.surface.copy(alpha = .9f), shape = CircleShape) {
                Text(" Ubah banner ", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(86.dp).clip(CircleShape).background(colors.surfaceVariant)
                    .border(2.dp, colors.primary, CircleShape).clickable { pickBanner = false; imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) Image(avatarBitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(62.dp), tint = colors.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(name.ifBlank { "Nama akun" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(username.ifBlank { "@username" }, color = colors.onSurfaceVariant)
                Text("Ubah foto profil", color = colors.primary, style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clickable { pickBanner = false; imagePicker.launch("image/*") }.padding(top = 4.dp))
            }
        }

        Surface(shape = RoundedCornerShape(22.dp), color = colors.surfaceVariant) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Informasi profil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(name, { name = it }, label = { Text("Nama") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(username, { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(bio, { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5)
                Button(
                    onClick = {
                        try {
                            config.parentFile?.mkdirs()
                            val json = JSONObject().put("name", name.trim()).put("username", username.trim())
                                .put("bio", bio.trim()).put("banner", bannerPath).put("profilePicture", picturePath)
                                .put("updatedAt", System.currentTimeMillis())
                            config.writeText(json.toString(2))
                            status = "Profil berhasil disimpan ke ${config.absolutePath}"
                        } catch (e: Exception) {
                            status = "Gagal menyimpan. Android mungkin memerlukan izin penyimpanan: ${e.message ?: "akses ditolak"}"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Simpan profil")
                }
            }
        }
        Text(status, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        Text("Lokasi konfigurasi: ${config.absolutePath}", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        Text("Gambar: .config/account/banner.png dan profile-pictures.png", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
    }
}

@Composable
fun ThemeTabContent(
    padding: PaddingValues,
    selectedTheme: RexThemeOption,
    onOpenTheme: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Tema", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Personalisasikan tampilan RexAps dengan 30 pilihan tema.", color = colors.onSurfaceVariant)
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenTheme),
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(
                    androidx.compose.ui.graphics.Brush.linearGradient(listOf(colors.primary, colors.tertiary))
                ), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ColorLens, null, tint = colors.onPrimary, modifier = Modifier.size(30.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Tema aktif", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    Text(selectedTheme.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Ketuk untuk memilih tema", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}
