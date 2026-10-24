package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun RexProfileRoot(padding: PaddingValues) {
    val ctl = rememberRexProfileController()
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Box(Modifier.fillMaxSize().padding(padding)) {
        if (isLandscape) {
            RexAccountScreenLandscape(ctl)
        } else {
            RexAccountScreenPortrait(ctl)
        }

        Surface(
            Modifier.align(Alignment.TopEnd).padding(14.dp),
            shape = CircleShape,
            color = Color.Black.copy(.35f),
            border = BorderStroke(1.dp, Color.White.copy(.18f))
        ) {
            IconButton(onClick = { ctl.showEdit = true }) {
                Icon(Icons.Default.PhotoCamera, null, tint = Color.White)
            }
        }
    }

    RexProfileOverlays(ctl)
}

@Composable
internal fun RexProfileOverlays(ctl: RexProfileController) {
    if (ctl.showEdit) EditProfileSheet(ctl)

    if (ctl.showStoryCompose) {
        StoryComposeDialog(
            onDismiss = { ctl.showStoryCompose = false },
            onText = { text, g ->
                ctl.showStoryCompose = false
                ctl.postStory(text, g)
            },
            onVideo = {
                ctl.showStoryCompose = false
                ctl.onPickStoryVideo()
            }
        )
    }

    ctl.storyIndex?.let { idx ->
        StoryViewer(
            acct = ctl.acct,
            rev = ctl.rev,
            stories = ctl.stories,
            start = idx,
            onDelete = { ctl.deleteStory(it) },
            onClose = { ctl.storyIndex = null }
        )
    }

    ctl.feedIndex?.let { idx ->
        VideoFeed(
            acct = ctl.acct,
            rev = ctl.rev,
            files = ctl.videos,
            start = idx,
            onDelete = { ctl.deleteVideo(it) },
            onClose = { ctl.feedIndex = null }
        )
    }

    ctl.crop?.let { req ->
        CropDialog(
            req = req,
            aspect = if (req.target == CropTarget.AVATAR) 1f else BANNER_ASPECT,
            circle = req.target == CropTarget.AVATAR,
            title = if (req.target == CropTarget.AVATAR) "Crop foto profil" else "Crop banner",
            onCancel = {
                ctl.crop = null
                req.video?.file?.delete()
            },
            onConfirm = { bmp, s, ox, oy, st -> ctl.applyCrop(req, bmp, s, ox, oy, st) }
        )
    }

    ctl.busy?.let { msg ->
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 6.dp) {
                Row(
                    Modifier.padding(22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    Text(msg)
                }
            }
        }
    }
}

@Composable
internal fun ProfileIdentity(acct: RexLocalAccount, centered: Boolean) {
    val colors = MaterialTheme.colorScheme
    val align = if (centered) Alignment.CenterHorizontally else Alignment.Start
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = align,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            acct.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = textAlign
        )
        Surface(shape = CircleShape, color = colors.primary.copy(.14f)) {
            Text(
                acct.username,
                Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = colors.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (acct.bio.isNotBlank()) {
            Text(
                acct.bio,
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = textAlign
            )
        }
    }
}

@Composable
internal fun ProfileStats(ctl: RexProfileController) {
    val colors = MaterialTheme.colorScheme
    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(ctl.stories.size, "Story", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(32.dp).background(colors.outlineVariant.copy(.6f)))
            StatItem(ctl.videos.size, "Video", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(32.dp).background(colors.outlineVariant.copy(.6f)))
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(Icons.Default.Lock, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Text("100% Lokal", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}
