package com.rexaps.rexmanager

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun PermissionGate(onGrant: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Folder, null, tint = Rex.folder, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            "Izinkan akses semua file",
            color = Rex.onSurface, fontSize = 20.sp, fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "RexManager butuh izin ini untuk menampilkan, mengompres, dan mengedit file kamu.",
            color = Rex.muted, fontSize = 14.sp
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrant, colors = ButtonDefaults.buttonColors(containerColor = Rex.primary)) {
            Text("Buka pengaturan")
        }
    }
}

// ───────────────────────── Shared UI ─────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RexTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(title, color = Rex.onSurface, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, color = Rex.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Kembali", tint = Rex.onSurface)
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Rex.bg)
    )
}

@Composable
internal fun iconFor(f: File): Pair<ImageVector, Color> {
    val e = f.extension.lowercase()
    return when {
        f.isDirectory -> Icons.Filled.Folder to Rex.folder
        e in IMAGE_EXT -> Icons.Filled.ImageIcon to Color(0xFFB05CFF)
        e in AUDIO_EXT -> Icons.Filled.MusicNote to Color(0xFF3BC4F5)
        e in VIDEO_EXT -> Icons.Filled.Movie to Color(0xFFEF5350)
        e in ARCHIVE_EXT -> Icons.Filled.Inventory2 to Color(0xFFF5883B)
        else -> Icons.Filled.InsertDriveFile to Rex.muted
    }
}

@Composable
internal fun drawerItemColors() = androidx.compose.material3.NavigationDrawerItemDefaults.colors(
    selectedContainerColor = Rex.primary.copy(alpha = 0.18f),
    selectedIconColor = Color(0xFF83A5FF),
    selectedTextColor = Rex.onSurface,
    unselectedContainerColor = Color.Transparent,
    unselectedIconColor = Rex.muted,
    unselectedTextColor = Rex.onSurface
)

@Composable
internal fun ArchiveScreen(file: File, onBack: () -> Unit) {
    var entries by remember { mutableStateOf<List<ArcItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            runCatching { listArchive(file) }
                .onSuccess { entries = it }
                .onFailure { error = it.message ?: "Arsip tidak bisa dibaca" }
        }
    }

    Column(Modifier.fillMaxSize()) {
        RexTopBar(file.name, "${formatSize(file.length())} • isi arsip", onBack)
        val list = entries
        when {
            error != null -> Text(
                "Tidak bisa membuka arsip: $error",
                color = Rex.danger, modifier = Modifier.padding(16.dp)
            )
            list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            list.isEmpty() -> Text("Arsip kosong", color = Rex.muted, modifier = Modifier.padding(16.dp))
            else -> LazyColumn {
                items(list) { entry ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (entry.isDir) Icons.Filled.Folder else Icons.Filled.InsertDriveFile,
                            null,
                            tint = if (entry.isDir) Rex.folder else Rex.muted
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            entry.name, color = Rex.onSurface, modifier = Modifier.weight(1f),
                            maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp
                        )
                        if (!entry.isDir) Text(formatSize(entry.size), color = Rex.muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ───────────────────────── Media (jpg/png/jpeg/mp3/ogg/mp4) ─────────────────────────
@Composable
internal fun MediaScreen(file: File, onBack: () -> Unit) {
    val ext = file.extension.lowercase()
    Column(Modifier.fillMaxSize()) {
        RexTopBar(file.name, formatSize(file.length()), onBack)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (ext) {
                in IMAGE_EXT -> ImagePreview(file)
                in AUDIO_EXT -> AudioPreview(file)
                in VIDEO_EXT -> VideoPreview(file)
            }
        }
    }
}

@Composable
internal fun ImagePreview(file: File) {
    val bmp by produceState<Bitmap?>(null, file) {
        value = withContext(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            var s = 1
            while (bounds.outWidth / s > 2048 || bounds.outHeight / s > 2048) s *= 2
            BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = s })
        }
    }
    val b = bmp
    if (b != null) {
        Image(b.asImageBitmap(), file.name, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    } else {
        CircularProgressIndicator()
    }
}
