package com.rexaps.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(
        modifier,
        shape = RoundedCornerShape(22.dp),
        color = c.surface.copy(alpha = .55f),
        border = BorderStroke(1.dp, c.outlineVariant.copy(alpha = .5f))
    ) { Column(content = content) }
}

@Composable
internal fun Reveal(visible: Boolean, delayMs: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible,
        enter = fadeIn(tween(550, delayMs)) + slideInVertically(tween(550, delayMs)) { it / 4 }
    ) { content() }
}

@Composable
internal fun Aurora(modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    val inf = rememberInfiniteTransition(label = "aurora")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "t")
    Box(modifier.drawBehind {
        drawRect(
            Brush.radialGradient(
                listOf(c.primary.copy(.26f), Color.Transparent),
                center = Offset(size.width * (.15f + .6f * t), size.height * .30f),
                radius = size.width * .95f
            )
        )
        drawRect(
            Brush.radialGradient(
                listOf(c.tertiary.copy(.22f), Color.Transparent),
                center = Offset(size.width * (.9f - .6f * t), size.height * .75f),
                radius = size.width * .9f
            )
        )
    })
}

@Composable
internal fun GlowBadge(text: String) {
    Surface(shape = CircleShape, color = Color.White.copy(.16f)) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun rememberRexProfileController(): RexProfileController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ctl = remember { RexProfileController(context, scope) }

    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        ctl.handlePick(it, CropTarget.BANNER)
    }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        ctl.handlePick(it, CropTarget.AVATAR)
    }
    val storyVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        ctl.addStoryVideo(it)
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        ctl.addVideo(it)
    }

    ctl.onPickBanner = { bannerPicker.launch(arrayOf("image/*", "video/*")) }
    ctl.onPickAvatar = { avatarPicker.launch(arrayOf("image/*", "video/*")) }
    ctl.onPickStoryVideo = { storyVideoPicker.launch("video/*") }
    ctl.onPickVideo = { videoPicker.launch(arrayOf("video/mp4", "image/png", "image/jpeg")) }

    LaunchedEffect(Unit) { ctl.load() }
    return ctl
}

@Composable
internal fun StatItem(value: Int, label: String, modifier: Modifier) {
    val v by animateIntAsState(value, tween(900), label = "stat")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$v", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun RexGrid(
    count: Int,
    columns: Int,
    cellAspect: Float,
    gap: Dp = 8.dp,
    cell: @Composable (Int, Modifier) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        for (r in 0 until (count + columns - 1) / columns) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (c in 0 until columns) {
                    val i = r * columns + c
                    if (i < count) cell(i, Modifier.weight(1f).aspectRatio(cellAspect))
                    else Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun AddTile(modifier: Modifier, title: String, sub: String, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .pressable(onClick)
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.primary.copy(.20f), c.tertiary.copy(.14f))))
            .border(1.dp, c.primary.copy(.35f), shape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(c.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = c.onPrimary, modifier = Modifier.size(22.dp))
            }
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
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
internal fun ProfileActions(ctl: RexProfileController) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = { ctl.showEdit = true },
            Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Edit profil")
        }
        FilledTonalButton(
            onClick = { ctl.showStoryCompose = true },
            Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Buat story")
        }
    }
}
