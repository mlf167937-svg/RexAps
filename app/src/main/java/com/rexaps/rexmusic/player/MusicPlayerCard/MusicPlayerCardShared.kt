package com.rexaps.rexmusic.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rexaps.rexmusic.*
import androidx.compose.material.icons.filled.Subtitles

@Composable
fun EqualizerBars(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    barWidth: Dp = 3.dp,
    height: Dp = 14.dp
) {
    val transition = rememberInfiniteTransition(label = "eq")
    val a by transition.animateFloat(
        0.3f, 1f, infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse), label = "a"
    )
    val b by transition.animateFloat(
        0.2f, 1f, infiniteRepeatable(tween(560, easing = LinearEasing), RepeatMode.Reverse), label = "b"
    )
    val c by transition.animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(340, easing = LinearEasing), RepeatMode.Reverse), label = "c"
    )
    Row(
        modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(a, b, c).forEach { fraction ->
            Box(
                Modifier.width(barWidth).fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(2.dp)).background(color)
            )
        }
    }
}

//  Cover 

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TrackInfo(
    title: String,
    artist: String,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    lyrics: Lyrics,
    positionMs: Long,
    onLyrics: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(
                title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.basicMarquee()
            )
            Spacer(Modifier.height(2.dp))
            Text(
                artist, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis
            )

            val activeLyric = activeLyricText(lyrics, positionMs)
            if (activeLyric != null || !lyrics.isEmpty) {
                Spacer(Modifier.height(9.dp))
                AnimatedVisibility(visible = activeLyric != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                            .clickable(onClick = onLyrics)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Crossfade(targetState = activeLyric, label = "currentLyric") { line ->
                            Text(
                                text = line.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                AnimatedVisibility(visible = activeLyric == null && !lyrics.isEmpty) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onLyrics)
                            .padding(horizontal = 2.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Subtitles,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Lihat lirik",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        LikeButton(isLiked, onToggleLike)
    }
}

internal fun activeLyricText(lyrics: Lyrics, positionMs: Long): String? {
    val list = lyrics.synced
    if (list.isEmpty()) return null
    var low = 0
    var high = list.lastIndex
    var active = -1
    while (low <= high) {
        val mid = (low + high) ushr 1
        if (list[mid].timeMs <= positionMs) {
            active = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return active.takeIf { it >= 0 }?.let { list[it].text.trim() }?.takeIf { it.isNotBlank() }
}

internal fun fmt(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
