// RexLocalAccountScreen.kt
package com.rexaps.ui

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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
internal fun AvatarContent(acct: RexLocalAccount, rev: Int, animate: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var sz by remember { mutableStateOf(IntSize.Zero) }
    val bmp by produceState<Bitmap?>(null, acct.picturePath, rev) {
        value = if (acct.picturePath.isBlank()) null
        else withContext(Dispatchers.IO) { decodeFileSampled(acct.picturePath, 768) }
    }
    val hasVideo = animate && acct.pictureVideo.isNotBlank() && File(acct.pictureVideo).exists()

    Box(
        modifier.clip(CircleShape).background(colors.surfaceVariant).onSizeChanged { sz = it },
        contentAlignment = Alignment.Center
    ) {
        val b = bmp
        if (b != null) Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Default.AccountCircle, null, Modifier.fillMaxSize(0.8f), tint = colors.primary)

        if (hasVideo) {
            RexVideo(
                acct.pictureVideo,
                Modifier.fillMaxSize().graphicsLayer {
                    scaleX = acct.pictureScale; scaleY = acct.pictureScale
                    translationX = acct.pictureX * sz.width
                    translationY = acct.pictureY * sz.height
                },
                key = rev
            )
        }
    }
}

@Composable
internal fun StoryRingAvatar(
    acct: RexLocalAccount,
    rev: Int,
    storyCount: Int,
    dia: Dp,
    modifier: Modifier,
    animate: Boolean = true,
    onClick: () -> Unit,
    onAdd: (() -> Unit)? = null
) {
    val c = MaterialTheme.colorScheme
    val inf = rememberInfiniteTransition(label = "ring")
    val angle by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "angle")
    val bob by inf.animateFloat(-3f, 3f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    var appear by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appear = true }
    val pop by animateFloatAsState(if (appear) 1f else .2f, spring(Spring.DampingRatioMediumBouncy), label = "pop")
    val ringColors = listOf(c.primary, c.tertiary, c.secondary, c.primary)

    Box(
        modifier
            .size(dia)
            .graphicsLayer {
                translationY = if (animate) bob * density else 0f
                scaleX = pop
                scaleY = pop
            }
            .drawBehind {
                val sw = 3.5.dp.toPx()
                val d = this.size.minDimension
                rotate(angle) {
                    val brush = Brush.sweepGradient(ringColors)
                    if (storyCount in 2..12) {
                        val seg = 360f / storyCount
                        for (i in 0 until storyCount) {
                            drawArc(
                                brush, -90f + i * seg + 5f, seg - 10f, false,
                                Offset(sw / 2, sw / 2), Size(d - sw, d - sw),
                                style = Stroke(sw, cap = StrokeCap.Round)
                            )
                        }
                    } else {
                        drawCircle(
                            brush, radius = d / 2 - sw / 2,
                            style = Stroke(sw),
                            alpha = if (storyCount == 0) .5f else 1f
                        )
                    }
                }
            }
    ) {
        AvatarContent(acct, rev, animate, Modifier.fillMaxSize().padding(7.dp).pressable(onClick))
        if (onAdd != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(c.primary)
                    .border(3.dp, c.background, CircleShape)
                    .pressable(onAdd),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = c.onPrimary, modifier = Modifier.size(16.dp))
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
