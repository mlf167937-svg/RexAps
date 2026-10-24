package com.rexaps.rexmusic

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class RexActions(
    val playSearch: (Int) -> Unit,
    val playHistory: (Int) -> Unit,
    val playMain: (Int) -> Unit,
    val playOffline: (RexTrack) -> Unit,
    val playOfflineAll: (Boolean) -> Unit,
    val playMix: () -> Unit,
    val playQueue: (Int) -> Unit,
    val addQueue: (RexTrack) -> Unit,
    val removeQueue: (Int) -> Unit,
    val moveQueueTop: (Int) -> Unit,
    val clearQueue: () -> Unit,
    val download: (RexTrack) -> Unit,
    val cancelDownload: (RexTrack) -> Unit,
    val delete: (RexTrack) -> Unit,
    val deleteAll: () -> Unit,
    val clearHistory: () -> Unit,
    val requestAccess: () -> Unit,
    val setOffline: (Boolean) -> Unit,
    val toggleAutoplay: () -> Unit,
    val searchArtist: (String) -> Unit,
    val goLibrary: () -> Unit,
    val goPlaylists: () -> Unit,
    val openMenu: (MenuTarget) -> Unit,
    val openPlaylistPicker: (RexTrack) -> Unit,
    val playPlaylist: (String) -> Unit,
    val playPlaylistTrack: (String, String) -> Unit,
    val createPlaylist: (String) -> Unit,
    val deletePlaylist: (String) -> Unit,
    val dismissError: () -> Unit
)

@Composable
fun rememberPressScale(source: MutableInteractionSource, pressed: Float = 0.96f): State<Float> {
    val isPressed by source.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
}

//  Basic building blocks 

/** Tombol ikon bulat dengan ukuran pasti (tidak pernah melebar / bertabrakan). */
@Composable
fun RexIconButton(
    icon: ImageVector,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    background: Color = Color.Unspecified,
    enabled: Boolean = true
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .then(if (background.isSpecified) Modifier.background(background) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) tint else tint.copy(alpha = 0.38f),
            modifier = Modifier.size(iconSize)
        )
    }
}
