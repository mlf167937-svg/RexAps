package com.rexaps.rexmusic.player

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.rexaps.rexmusic.*
import kotlin.math.roundToInt

//  Seek 

@Composable
internal fun SeekSection(
    positionState: State<Long>,
    durationMs: Long,
    bufferedPercent: Int,
    busy: Boolean,
    onSeek: (Float) -> Unit
) {
    val positionMs = positionState.value
    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
    val muted = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    Column {
        if (durationMs <= 0L && busy) {
            Box(Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.onBackground, trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                )
            }
        } else {
            SeekBar(progress.coerceIn(0f, 1f), bufferedPercent / 100f, onSeek)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmt(positionMs), style = MaterialTheme.typography.labelSmall, color = muted)
            Text(
                if (durationMs > 0) fmt(durationMs) else "--:--",
                style = MaterialTheme.typography.labelSmall, color = muted
            )
        }
    }
}

@Composable
internal fun SeekBar(progress: Float, buffered: Float, onSeek: (Float) -> Unit) {
    val currentOnSeek by rememberUpdatedState(onSeek)
    var widthPx by remember { mutableFloatStateOf(1f) }
    val thumb = 14.dp
    Box(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(horizontal = thumb / 2)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { currentOnSeek((it.x / widthPx).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    currentOnSeek((change.position.x / widthPx).coerceIn(0f, 1f))
                }
            }
            .semantics {
                contentDescription = "Progres lagu"
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                setProgress { currentOnSeek(it.coerceIn(0f, 1f)); true }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f)))
        Box(Modifier.fillMaxWidth(buffered.coerceIn(0f, 1f)).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)))
        Box(Modifier.fillMaxWidth(progress).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onBackground))
        Box(
            Modifier
                .offset { IntOffset((progress * widthPx - thumb.toPx() / 2).roundToInt(), 0) }
                .size(thumb)
                .background(MaterialTheme.colorScheme.onBackground, CircleShape)
        )
    }
}
