package com.rexaps.rexmusic

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

//  Shared row helper 

@Composable
internal fun TrackRowFor(
    track: RexTrack,
    state: RexMusicUiState,
    actions: RexActions,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val key = remember(track.title, track.artist) { offlineKey(track) }
    val active = track.id == state.nowPlaying?.id
    TrackRow(
        track = track,
        active = active,
        playing = active && state.isPlaying,
        loading = active && state.phase.isPlayerBusy,
        downloaded = state.offline.isDownloaded(track, key),
        status = state.offline.downloads[key],
        subtitle = subtitle,
        onClick = onClick,
        onMore = { actions.openMenu(MenuTarget(track, onClick)) }
    )
}

@Composable
internal fun HomeHeader(greeting: String, offline: Boolean, onToggle: () -> Unit, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RexIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", onBack, size = 48.dp)
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(
                greeting, style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                "RexMusic", style = MaterialTheme.typography.headlineSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        ModeChip(offline, onToggle)
    }
}

@Composable
internal fun MixHeroCard(
    favorites: List<String>,
    popularCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtitle = when {
        favorites.isNotEmpty() -> "Berdasarkan ${favorites.joinToString(", ")}"
        popularCount > 0 -> "Acak dari $popularCount lagu populer"
        else -> "Putar beberapa lagu, Mix akan belajar seleramu"
    }
    val source = remember { MutableInteractionSource() }
    val scale = rememberPressScale(source, 0.98f)
    Box(
        modifier.fillMaxWidth()
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(RoundedCornerShape(20.dp))
            .background(RexHeroBrush)
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick)
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "MIX UNTUKMU", style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f), letterSpacing = 1.5.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Putar Mix Pintar", style = MaterialTheme.typography.headlineSmall,
                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(16.dp))
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow, contentDescription = "Putar Mix",
                    tint = RexPlayerBase, modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
internal fun SearchField(
    query: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                RexIconButton(
                    Icons.Default.Clear, "Hapus pencarian", onClear,
                    size = 40.dp, iconSize = 20.dp, tint = scheme.onSurfaceVariant
                )
            }
        },
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = scheme.surfaceVariant,
            unfocusedContainerColor = scheme.surfaceVariant,
            disabledContainerColor = scheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = scheme.primary
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
internal fun ExploreTile(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = rexTileColor(name)
    Box(
        modifier.height(88.dp).clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(c, lerp(c, Color.Black, 0.35f))))
            .clickable(onClick = onClick)
    ) {
        Text(
            name, style = MaterialTheme.typography.titleMedium, color = Color.White,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 12.dp, end = 48.dp)
        )
        Icon(
            Icons.Default.MusicNote, contentDescription = null,
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 8.dp, y = 8.dp)
                .size(56.dp).rotate(20f)
        )
    }
}

@Composable
internal fun PlaylistEmptyState(onCreate: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp)
            .clip(RoundedCornerShape(24.dp)).background(scheme.surfaceVariant).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(76.dp).clip(RoundedCornerShape(24.dp)).background(RexHeroBrush),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.QueueMusic, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
        }
        Text("Mulai koleksi musikmu", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            "Buat playlist untuk mengelompokkan lagu sesuai mood. Sampul playlist akan terbentuk otomatis dari thumbnail lagu di dalamnya.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onCreate, shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Buat playlist pertama")
        }
    }
}
