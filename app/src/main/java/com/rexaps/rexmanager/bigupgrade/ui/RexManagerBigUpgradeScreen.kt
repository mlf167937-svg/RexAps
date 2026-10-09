package com.rexaps.rexmanager.bigupgrade.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexmanager.bigupgrade.editor.RexCodeEditorScreen

private val Navy = Color(0xFF07111F)
private val Panel = Color(0xFF101E31)
private val Panel2 = Color(0xFF17263D)
private val Blue = Color(0xFF318BFF)
private val Purple = Color(0xFF8957F5)
private val Muted = Color(0xFF8DA0B8)

private enum class Page { HOME, FILES, CLOUD, TOOLS, EDITOR, SETTINGS }

/** New full-screen shell. File operations are handed off to existing RexManager entry points. */
@Composable
fun RexManagerBigUpgradeScreen(
    onOpenLegacyFiles: () -> Unit = {},
    onOpenCloud: () -> Unit = {},
    onOpenArchive: () -> Unit = {},
    onExit: () -> Unit = {}
) {
    var page by remember { mutableStateOf(Page.HOME) }
    var onboarding by remember { mutableStateOf(true) }
    var drag by remember { mutableFloatStateOf(0f) }
    val progress by animateFloatAsState((drag / 180f).coerceIn(0f, 1f), label = "swipe-progress")

    if (onboarding) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF030914), Color(0xFF0C1B33), Color(0xFF211044)))).padding(24.dp)) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(118.dp).clip(RoundedCornerShape(34.dp)).background(Brush.linearGradient(listOf(Blue, Purple))), contentAlignment = Alignment.Center) {
                    Text("R", fontSize = 84.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
                Spacer(Modifier.height(26.dp))
                Text("RexManager", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("File Manager Next Level", color = Color(0xFF93C5FD), fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                Text("Kelola file, arsip, kode, dan cloud\ndalam satu aplikasi.", color = Color(0xFFCBD5E1), fontSize = 14.sp, lineHeight = 22.sp)
            }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Geser untuk mulai", color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(58.dp).clip(CircleShape).background(Color.White.copy(alpha = .12f)).pointerInput(Unit) {
                    detectHorizontalDragGestures(onDragEnd = { if (drag > 130f) onboarding = false; drag = 0f }, onHorizontalDrag = { _, amount -> drag = (drag + amount).coerceIn(0f, 180f) })
                }) {
                    Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(Brush.horizontalGradient(listOf(Blue, Purple))))
                    Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { Icon(Icons.Default.ArrowForward, null, tint = Purple) }
                        Spacer(Modifier.weight(1f))
                        Text("SWIPE", color = Color.White.copy(alpha = .8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("RexManager · Kelola File Tanpa Batas", color = Muted, fontSize = 11.sp)
            }
        }
        return
    }

    if (page == Page.EDITOR) {
        RexCodeEditorScreen(onBack = { page = Page.HOME })
        return
    }

    Scaffold(containerColor = Navy, bottomBar = {
        NavigationBar(containerColor = Panel, contentColor = Color.White) {
            NavItem("Beranda", Icons.Default.Home, page == Page.HOME) { page = Page.HOME }
            NavItem("Folder", Icons.Default.Folder, page == Page.FILES) { page = Page.FILES; onOpenLegacyFiles() }
            NavItem("Cloud", Icons.Default.Cloud, page == Page.CLOUD) { page = Page.CLOUD; onOpenCloud() }
            NavItem("Alat", Icons.Default.GridView, page == Page.TOOLS) { page = Page.TOOLS }
        }
    }) { padding ->
        AnimatedContent(page, modifier = Modifier.fillMaxSize().padding(padding), label = "rex-page") { selected ->
            when (selected) {
                Page.HOME -> HomePage(onFiles = { page = Page.FILES; onOpenLegacyFiles() }, onEditor = { page = Page.EDITOR }, onCloud = { page = Page.CLOUD; onOpenCloud() }, onTools = { page = Page.TOOLS })
                Page.FILES -> SimplePage("Penjelajah File", "Operasi file memakai mesin RexManager yang sudah ada.", Icons.Default.FolderOpen, onOpenLegacyFiles)
                Page.CLOUD -> SimplePage("Google Drive", "Hubungkan akun untuk melihat file cloud.", Icons.Default.Cloud, onOpenCloud)
                Page.TOOLS -> ToolsPage(onEditor = { page = Page.EDITOR }, onArchive = onOpenArchive, onSettings = { page = Page.SETTINGS })
                Page.SETTINGS -> SimplePage("Pengaturan", "Tema, tampilan, keamanan, dan informasi aplikasi.", Icons.Default.Settings, onExit)
                Page.EDITOR -> Unit
            }
        }
    }
}

@Composable private fun NavItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.weight(1f).clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) Blue else Muted)
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 11.sp, color = if (selected) Blue else Muted)
    }
}

@Composable private fun HomePage(onFiles: () -> Unit, onEditor: () -> Unit, onCloud: () -> Unit, onTools: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Blue, Purple))), contentAlignment = Alignment.Center) { Text("R", color = Color.White, fontWeight = FontWeight.Black, fontSize = 25.sp) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("RexManager", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp); Text("Kelola File Tanpa Batas", color = Muted, fontSize = 11.sp) }; IconButton(onClick = onTools) { Icon(Icons.Default.Settings, null, tint = Color.White) } } }
        item { Surface(color = Panel, shape = RoundedCornerShape(22.dp)) { Column(Modifier.fillMaxWidth().padding(18.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Storage, null, tint = Blue); Spacer(Modifier.width(8.dp)); Text("Penyimpanan Internal", color = Color.White, fontWeight = FontWeight.SemiBold) }; Spacer(Modifier.height(14.dp)); Text("Akses file dan folder perangkat", color = Muted, fontSize = 12.sp); Spacer(Modifier.height(12.dp)); LinearProgressIndicator(progress = { .42f }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = Blue, trackColor = Panel2); Spacer(Modifier.height(8.dp)); Text("Buka penjelajah untuk melihat penggunaan aktual", color = Muted, fontSize = 11.sp); Spacer(Modifier.height(12.dp)); Button(onClick = onFiles, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("Buka Folder") } } } }
        item { Text("Kategori", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Category("Dokumen", Icons.Default.Description, Color(0xFF4285F4), Modifier.weight(1f), onFiles); Category("Gambar", Icons.Default.Image, Purple, Modifier.weight(1f), onFiles); Category("Video", Icons.Default.Movie, Color(0xFFEC4899), Modifier.weight(1f), onFiles) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Category("Audio", Icons.Default.MusicNote, Color(0xFF10B981), Modifier.weight(1f), onFiles); Category("Arsip", Icons.Default.FolderZip, Color(0xFFF59E0B), Modifier.weight(1f), onTools); Category("Kode", Icons.Default.Code, Color(0xFF06B6D4), Modifier.weight(1f), onEditor) } }
        item { Text("Akses Cepat", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
        item { QuickAction("RexCode Editor", "Edit teks dengan syntax highlighting", Icons.Default.Code, onEditor) }
        item { QuickAction("Google Drive", "Akses file cloud", Icons.Default.Cloud, onCloud) }
        item { QuickAction("Alat & Arsip", "Kompres, ekstrak, dan utilitas", Icons.Default.Archive, onTools) }
    }
}

@Composable private fun Category(name: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(98.dp), color = Panel, shape = RoundedCornerShape(18.dp)) { Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center) { Icon(icon, null, tint = tint); Spacer(Modifier.height(9.dp)); Text(name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) } }
}
@Composable private fun QuickAction(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Panel, shape = RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Blue, modifier = Modifier.size(28.dp)); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Muted, fontSize = 12.sp) }; Icon(Icons.Default.ChevronRight, null, tint = Muted) } }
}
@Composable private fun ToolsPage(onEditor: () -> Unit, onArchive: () -> Unit, onSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Alat", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold); QuickAction("RexCode", "Editor teks internal", Icons.Default.Code, onEditor); QuickAction("Kompres & Ekstrak", "Gunakan dukungan arsip yang tersedia", Icons.Default.Archive, onArchive); QuickAction("Pengaturan", "Preferensi aplikasi", Icons.Default.Settings, onSettings) }
}
@Composable private fun SimplePage(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onAction: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(icon, null, tint = Blue, modifier = Modifier.size(54.dp)); Spacer(Modifier.height(16.dp)); Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text(description, color = Muted); Spacer(Modifier.height(18.dp)); Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("Buka") } }
}
