package com.rexaps.rextools.tiktok

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.rexaps.rextools.downloader.MediaDownloader
import kotlinx.coroutines.launch

private val RSm = RoundedCornerShape(14.dp)
private val RMd = RoundedCornerShape(18.dp)
private val RLg = RoundedCornerShape(28.dp)

@Composable
private fun accentBrush(): Brush {
    val s = MaterialTheme.colorScheme
    return Brush.linearGradient(listOf(s.primary, s.tertiary))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiktokDownloaderScreen(
    onBack: () -> Unit = {},
    vm: TiktokDownloaderViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = scheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "TikTok Downloader",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = scheme.background)
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .shadow(12.dp, RMd, ambientColor = scheme.primary, spotColor = scheme.primary)
                        .clip(RMd)
                        .background(accentBrush()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = scheme.onPrimary)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Unduh video & foto TikTok",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        "Tempel link untuk mengambil video, foto, atau musiknya.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            // Input
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.url,
                    onValueChange = vm::onUrlChange,
                    label = { Text("URL TikTok") },
                    placeholder = { Text("https://vt.tiktok.com/xxx/") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    trailingIcon = {
                        if (state.url.isNotEmpty()) {
                            IconButton(onClick = { vm.onUrlChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus URL")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RMd,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        focusManager.clearFocus()
                        if (!state.loading) vm.download()
                    }),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        vm.download()
                    },
                    enabled = !state.loading,
                    shape = RMd,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = scheme.onPrimary,
                        disabledContainerColor = scheme.surfaceContainerHigh,
                        disabledContentColor = scheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .then(if (!state.loading) Modifier.background(accentBrush()) else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = scheme.primary
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Memproses…", fontWeight = FontWeight.SemiBold)
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Ambil Media", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Error
            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                state.error?.let { ErrorBanner(it) }
            }

            // Result
            state.result?.let { r ->
                ResultCard(
                    result = r,
                    onOpen = { url -> ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                    onDownloadVideo = { url ->
                        scope.launch {
                            MediaDownloader.downloadVideo(ctx, url)
                                .onSuccess { Toast.makeText(ctx, "Video tersimpan", Toast.LENGTH_SHORT).show() }
                                .onFailure { Toast.makeText(ctx, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show() }
                        }
                    },
                    onDownloadImage = { url ->
                        scope.launch {
                            MediaDownloader.downloadImage(ctx, url)
                                .onSuccess { Toast.makeText(ctx, "Gambar tersimpan", Toast.LENGTH_SHORT).show() }
                                .onFailure { Toast.makeText(ctx, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show() }
                        }
                    },
                    onDownloadMusic = { url ->
                        scope.launch {
                            MediaDownloader.downloadVideo(ctx, url, "tiktok_music_${System.currentTimeMillis()}.mp4")
                                .onSuccess { Toast.makeText(ctx, "Musik tersimpan", Toast.LENGTH_SHORT).show() }
                                .onFailure { Toast.makeText(ctx, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show() }
                        }
                    }
                )
            }

            if (state.result == null && state.error == null && !state.loading) {
                EmptyHint()
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RMd,
        color = scheme.errorContainer,
        contentColor = scheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null)
            Column {
                Text("Gagal mengambil media", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(message, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EmptyHint() {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RLg)
            .border(BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.6f)), RLg)
            .padding(vertical = 36.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(scheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Link, contentDescription = null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
        }
        Text("Belum ada media", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            "Tempel link TikTok lalu tekan Ambil Media.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ResultCard(
    result: com.rexaps.rextools.TiktokDownloadResult,
    onOpen: (String) -> Unit,
    onDownloadVideo: (String) -> Unit,
    onDownloadImage: (String) -> Unit,
    onDownloadMusic: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Card(
        shape = RLg,
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Author
            result.author?.let { a ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = a.avatar,
                        contentDescription = a.nickname?.let { "Foto profil $it" } ?: "Foto profil",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(2.dp, scheme.primary.copy(alpha = 0.6f), CircleShape)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(scheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            a.nickname ?: "-",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "@${a.fullname ?: "-"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Title
            result.title?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Cover
            result.cover?.let { cover ->
                MediaFrame {
                    AsyncImage(
                        model = cover,
                        contentDescription = "Sampul konten",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Video case
            if (result.isVideo && !result.videoUrl.isNullOrBlank()) {
                val videoUrl = result.videoUrl
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { onOpen(videoUrl) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Putar", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onDownloadVideo(videoUrl) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Video", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Photo case
            if (!result.isVideo && result.photoUrls.isNotEmpty()) {
                val photos = result.photoUrls
                Text(
                    "Foto (${photos.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() }
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    photos.forEachIndexed { i, imgUrl ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            MediaFrame {
                                AsyncImage(
                                    model = imgUrl,
                                    contentDescription = "Foto ${i + 1} dari ${photos.size}",
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            FilledTonalButton(
                                onClick = { onDownloadImage(imgUrl) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Simpan foto ${i + 1}", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Music
            result.music_info?.let { m ->
                Surface(
                    shape = RMd,
                    color = scheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier.size(42.dp).clip(RSm).background(accentBrush()),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(22.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                m.title ?: "-",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                m.author ?: "-",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        m.url?.takeIf { it.isNotBlank() }?.let { url ->
                            IconButton(onClick = { onDownloadMusic(url) }) {
                                Icon(Icons.Default.Download, contentDescription = "Simpan musik")
                            }
                        }
                    }
                }
            }

            // Stats
            result.stats?.let { s ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatBox(Icons.Default.Visibility, "Views", s.views ?: "-", Modifier.weight(1f))
                    StatBox(Icons.Default.FavoriteBorder, "Likes", s.likes ?: "-", Modifier.weight(1f))
                    StatBox(Icons.Default.ChatBubbleOutline, "Comment", s.comment ?: "-", Modifier.weight(1f))
                    StatBox(Icons.Default.Share, "Share", s.share ?: "-", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MediaFrame(content: @Composable () -> Unit) {
    Surface(
        shape = RMd,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.clip(RMd), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun StatBox(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RMd,
        color = scheme.surfaceContainerHigh,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $value" }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
