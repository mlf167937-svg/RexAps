package com.rexaps.rexmanager

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.io.File
import kotlinx.coroutines.launch

@Composable
internal fun __splitImpl_RexManagerScreen(onExit: () -> Unit = {}) {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(hasAccess()) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) granted = hasAccess()
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val stack = remember { mutableStateListOf<Route>(Route.Home) }
    val route = stack.last()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun navigateFromDrawer(target: Route) {
        stack.clear()
        stack.add(target)
        scope.launch { drawerState.close() }
    }

    BackHandler {
        if (stack.size > 1) stack.removeAt(stack.lastIndex) else onExit()
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun open(f: File) {
        val ext = f.extension.lowercase()
        stack.add(
            when {
                f.isDirectory -> Route.Folder(f.path)
                ext in ARCHIVE_EXT -> Route.Archive(f.path)
                ext in IMAGE_EXT || ext in AUDIO_EXT || ext in VIDEO_EXT -> Route.Media(f.path)
                else -> Route.Editor(f.path)
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = granted,
        scrimColor = Color(0x99050A16),
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Rex.surface,
                drawerShape = RoundedCornerShape(topEnd = 26.dp, bottomEnd = 26.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Rex.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(27.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("RexManager", color = Rex.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("RexStudio File Explorer", color = Rex.muted, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Text("NAVIGASI", color = Rex.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                    NavigationDrawerItem(
                        label = { Text("Beranda") },
                        selected = route == Route.Home,
                        onClick = { navigateFromDrawer(Route.Home) },
                        icon = { Icon(Icons.Filled.Home, null) },
                        colors = drawerItemColors()
                    )
                    NavigationDrawerItem(
                        label = { Text("Penyimpanan internal") },
                        selected = route is Route.Folder && route.path == Environment.getExternalStorageDirectory().path,
                        onClick = { navigateFromDrawer(Route.Folder(Environment.getExternalStorageDirectory().path)) },
                        icon = { Icon(Icons.Filled.Storage, null) },
                        colors = drawerItemColors()
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("KATEGORI", color = Rex.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                    val root = Environment.getExternalStorageDirectory()
                    listOf(
                        Triple("Download", File(root, "Download"), Icons.Filled.Download),
                        Triple("Gambar", File(root, "Pictures"), Icons.Filled.ImageIcon),
                        Triple("Video", File(root, "Movies"), Icons.Filled.Movie),
                        Triple("Musik", File(root, "Music"), Icons.Filled.MusicNote),
                        Triple("Dokumen", File(root, "Documents"), Icons.Filled.Description)
                    ).forEach { (label, file, icon) ->
                        NavigationDrawerItem(
                            label = { Text(label) },
                            selected = route is Route.Folder && route.path == file.path,
                            onClick = { navigateFromDrawer(Route.Folder(file.path)) },
                            icon = { Icon(icon, null) },
                            colors = drawerItemColors()
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(color = Rex.surfaceHigh, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Dibuat untuk file kamu", color = Rex.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("Sederhana. Cepat. Terorganisir.", color = Rex.muted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    ) {
        Surface(Modifier.fillMaxSize(), color = Color.Transparent) {
            if (!granted) {
                PermissionGate {
                    ctx.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:${ctx.packageName}")
                        )
                    )
                }
            } else {
                when (route) {
                    Route.Home -> HomeScreen(
                        onOpen = { stack.add(Route.Folder(it)) },
                        onBack = onExit,
                        onMenu = { scope.launch { drawerState.open() } }
                    )
                    is Route.Folder -> FolderScreen(
                        path = route.path,
                        onBack = ::pop,
                        onOpen = ::open,
                        onCompress = { stack.add(Route.Compress(it)) }
                    )
                    is Route.Archive -> ArchiveScreen(File(route.path), ::pop)
                    is Route.Editor -> EditorScreen(File(route.path), ::pop)
                    is Route.Media -> MediaScreen(File(route.path), ::pop)
                    is Route.Compress -> CompressScreen(route.paths.map { File(it) }, ::pop)
                }
            }
        }
    }
}
