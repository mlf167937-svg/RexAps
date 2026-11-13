package com.rexaps.rexmusic

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexMusicAddSheet(
    onDismiss: () -> Unit,
    onAddManual: (String, String, String, RexTrackSource, String, Uri?) -> Unit,
    onPickLocalAudio: (String, String, RexTrackSource, String, Uri?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var source by remember { mutableStateOf(RexTrackSource.SPOTIFY) }
    var sourceExpanded by remember { mutableStateOf(false) }
    var thumbnailUrl by remember { mutableStateOf("") }
    var thumbnailUri by remember { mutableStateOf<Uri?>(null) }
    var thumbnailName by remember { mutableStateOf("") }
    var invalidLink by remember { mutableStateOf(false) }
    var invalidThumbnail by remember { mutableStateOf(false) }

    val thumbnailPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            thumbnailUri = uri
            thumbnailUrl = ""
            thumbnailName = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            }.getOrNull()?.takeIf { it.isNotBlank() }?.takeLast(42) ?: "Foto dipilih"
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            invalidThumbnail = false
        }
    }

    fun validThumbnailUrl(value: String): Boolean {
        if (value.isBlank()) return true
        val uri = runCatching { java.net.URI(value.trim()) }.getOrNull() ?: return false
        val path = uri.path.orEmpty().lowercase()
        return uri.scheme in setOf("http", "https") &&
            !uri.host.isNullOrBlank() &&
            (path.endsWith(".jpg") || path.endsWith(".jpeg"))
    }

    fun chooseFile() {
        if (!validThumbnailUrl(thumbnailUrl)) {
            invalidThumbnail = true
            return
        }
        onPickLocalAudio(title.trim(), artist.trim(), source, thumbnailUrl.trim(), thumbnailUri)
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, bottom = 28.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Tambah musik", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Simpan musik dengan metadata milikmu",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup")
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Nama Music") },
                shape = RoundedCornerShape(15.dp)
            )

            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Nama Penyanyi") },
                shape = RoundedCornerShape(15.dp)
            )

            OutlinedTextField(
                value = link,
                onValueChange = { link = it; invalidLink = false },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Link Music/File") },
                placeholder = { Text("Tempel link atau pilih file MP3") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                isError = invalidLink,
                supportingText = if (invalidLink) ({ Text("Link tidak sesuai dengan sumber yang dipilih") }) else null,
                trailingIcon = {
                    IconButton(onClick = ::chooseFile) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Pilih file MP3")
                    }
                },
                shape = RoundedCornerShape(15.dp)
            )

            Box {
                OutlinedButton(
                    onClick = { sourceExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(15.dp),
                    contentPadding = PaddingValues(horizontal = 15.dp)
                ) {
                    Text("Source dari: ${source.label}", modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Pilih sumber")
                }
                DropdownMenu(
                    expanded = sourceExpanded,
                    onDismissRequest = { sourceExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.86f)
                ) {
                    listOf(
                        RexTrackSource.SPOTIFY,
                        RexTrackSource.YOUTUBE,
                        RexTrackSource.APPLE_MUSIC,
                        RexTrackSource.LOCAL
                    ).forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.label) },
                            onClick = {
                                source = item
                                sourceExpanded = false
                                invalidLink = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = thumbnailUrl,
                onValueChange = {
                    thumbnailUrl = it
                    thumbnailUri = null
                    thumbnailName = ""
                    invalidThumbnail = false
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Pilih Thumbnail") },
                placeholder = { Text("Foto atau link .jpg") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                isError = invalidThumbnail,
                supportingText = when {
                    invalidThumbnail -> ({ Text("Gunakan link gambar .jpg/.jpeg yang valid") })
                    thumbnailName.isNotBlank() -> ({ Text("Foto dipilih: $thumbnailName") })
                    else -> null
                },
                trailingIcon = {
                    IconButton(onClick = { thumbnailPicker.launch(arrayOf("image/*")) }) {
                        Icon(Icons.Default.Image, contentDescription = "Pilih foto thumbnail")
                    }
                },
                shape = RoundedCornerShape(15.dp)
            )

            Button(
                onClick = {
                    if (title.isBlank() || artist.isBlank()) return@Button
                    if (!validThumbnailUrl(thumbnailUrl)) {
                        invalidThumbnail = true
                        return@Button
                    }
                    if (source == RexTrackSource.LOCAL) {
                        chooseFile()
                    } else if (RexMusicCatalogApi.isValidProviderLink(source, link)) {
                        onAddManual(title.trim(), artist.trim(), link.trim(), source, thumbnailUrl.trim(), thumbnailUri)
                        onDismiss()
                    } else {
                        invalidLink = true
                    }
                },
                enabled = title.isNotBlank() && artist.isNotBlank() &&
                    (source == RexTrackSource.LOCAL || link.isNotBlank()),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(if (source == RexTrackSource.LOCAL) Icons.Default.MusicNote else Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (source == RexTrackSource.LOCAL) "Pilih file & simpan offline" else "Tambah musik")
            }
        }
    }
}
