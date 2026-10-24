package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun StoryViewer(
    acct: RexLocalAccount,
    rev: Int,
    stories: List<RexStory>,
    start: Int,
    onDelete: (RexStory) -> Unit,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    var index by remember { mutableIntStateOf(start) }
    val progress = remember { Animatable(0f) }
    var paused by remember { mutableStateOf(false) }
    var dur by remember(index) { mutableLongStateOf(0L) }
    val story = stories.getOrNull(index)
    if (story == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    LaunchedEffect(index) {
        progress.snapTo(0f)
        dur = if (story.isVideo) {
            withContext(Dispatchers.IO) { videoDurationOf(ctx, VSrc(null, story.file)) }.takeIf { it > 0 } ?: 15000L
        } else 5000L
    }
    LaunchedEffect(index, paused, dur) {
        if (dur > 0 && !paused) {
            val remaining = ((1f - progress.value) * dur).toInt().coerceAtLeast(200)
            progress.animateTo(1f, tween(remaining, easing = LinearEasing))
            if (index < stories.lastIndex) index++ else onClose()
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black)
                .pointerInput(index) {
                    detectTapGestures(
                        onPress = { paused = true; tryAwaitRelease(); paused = false },
                        onTap = { o ->
                            if (o.x < size.width / 3f) { if (index > 0) index-- }
                            else if (index < stories.lastIndex) index++ else onClose()
                        }
                    )
                }
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = { if (total > 160f) onClose() }
                    ) { _, d -> total += d }
                }
        ) {
            if (story.isVideo) {
                RexVideo(story.file.path, Modifier.fillMaxSize(), key = index, loop = false, muted = false, playing = !paused, fit = true)
            } else {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.linearGradient(storyGradients[story.gradient.mod(storyGradients.size)])
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        story.text,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }

            Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(.55f), Color.Transparent))))

            Column(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    stories.forEachIndexed { i, _ ->
                        val frac = when {
                            i < index -> 1f
                            i == index -> progress.value
                            else -> 0f
                        }
                        Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(Color.White.copy(.3f))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(Color.White))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AvatarContent(acct, rev, false, Modifier.size(38.dp).border(1.5.dp, Color.White, CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        val left = (DAY_MS - (System.currentTimeMillis() - story.time)).coerceAtLeast(0)
                        Text(acct.name, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Hilang ${left / 3_600_000}j ${left / 60_000 % 60}m lagi",
                            color = Color.White.copy(.75f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    IconButton(onClick = { onDelete(story); onClose() }) { Icon(Icons.Default.Delete, null, tint = Color.White) }
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.White) }
                }
            }
        }
    }
}
