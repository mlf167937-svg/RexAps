package com.rexaps.rexmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Overlay lirik premium: background foto cover (blur + scrim), piringan hitam berputar,
 * lirik synced dengan fade edge, tap baris untuk seek (opsional lewat [onSeekTo]).
 */
@Composable
fun RexLyricsOverlay(
    track: RexTrack,
    lyrics: Lyrics,
    loading: Boolean,
    error: String?,
    accent: Color?,
    positionState: State<Long>,
    durationMs: Long,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    onSeekTo: ((Long) -> Unit)? = null
) {
    val base = accent ?: MaterialTheme.colorScheme.primary

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(RexPlayerBase)
            .pointerInput(Unit) { }
    ) {
        val landscape = maxWidth > maxHeight && maxWidth >= 560.dp
        LyricsBackdrop(track.cover, base)

        if (landscape) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                LyricsHeader(onClose)
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        Modifier
                            .weight(0.82f)
                            .fillMaxHeight()
                    ) {
                        NowSpinning(track, isPlaying, base)
                        Spacer(Modifier.weight(1f))
                        LyricsFooter(positionState, durationMs, base, onSeekTo)
                    }
                    LyricsBody(
                        lyrics = lyrics,
                        loading = loading,
                        error = error,
                        positionState = positionState,
                        onSeekTo = onSeekTo,
                        modifier = Modifier.weight(1.18f).fillMaxHeight()
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                LyricsHeader(onClose)
                NowSpinning(track, isPlaying, base)
                LyricsBody(
                    lyrics = lyrics,
                    loading = loading,
                    error = error,
                    positionState = positionState,
                    onSeekTo = onSeekTo,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                LyricsFooter(positionState, durationMs, base, onSeekTo)
            }
        }
    }
}
