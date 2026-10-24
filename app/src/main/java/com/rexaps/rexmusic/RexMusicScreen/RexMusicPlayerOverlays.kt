package com.rexaps.rexmusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.rexaps.rexmusic.player.MusicPlayerCard
import com.rexaps.rexmusic.player.PlayerActions

@Composable
internal fun RexMusicPlayerOverlays(
    state: RexMusicUiState,
    positionState: androidx.compose.runtime.State<Long>,
    vm: RexMusicViewModel,
    showPlayer: Boolean,
    onShowPlayer: (Boolean) -> Unit,
    likedIds: MutableList<String>,
    onMenuTargetChange: (MenuTarget?) -> Unit,
    onShowRepeat: (Boolean) -> Unit,
    onShowQueue: (Boolean) -> Unit,
    onDeleteTarget: (RexTrack?) -> Unit,
    downloadTrack: (RexTrack) -> Unit
) {
    AnimatedVisibility(
        visible = showPlayer,
        enter = slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it / 12 } +
            scaleIn(
                initialScale = 0.965f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(tween(240, easing = FastOutSlowInEasing)),
        exit = slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { it / 10 } +
            scaleOut(targetScale = 0.985f, animationSpec = tween(200)) +
            fadeOut(tween(160))
    ) {
        val track = state.nowPlaying

        if (track != null) {
            var accent by remember(track.cover) {
                mutableStateOf<Color?>(null)
            }

            val dlKey = remember(track.title, track.artist) {
                offlineKey(track)
            }

            MusicPlayerCard(
                track = track,
                state = state,
                positionState = positionState,
                isLiked = track.id in likedIds,
                downloaded = state.offline.isDownloaded(
                    track,
                    dlKey
                ),
                downloadStatus = state.offline.downloads[dlKey],
                accent = accent,
                onAccentFound = {
                    accent = it
                },

                actions = PlayerActions(
                    onClose = {
                        onShowPlayer(false)
                    },

                    onLyrics = {
                        vm.toggleLyrics()
                    },

                    onMore = {
                        onMenuTargetChange(MenuTarget(track, null))
                    },

                    onPlayPause = {
                        vm.togglePlay()
                    },

                    onNext = {
                        vm.next()
                    },

                    onPrev = {
                        vm.prev()
                    },

                    onOpenPrevious = {
                        vm.prev(force = true)
                    },

                    onSeek = {
                        vm.seekTo(it)
                    },

                    onToggleLike = {
                        if (track.id in likedIds) likedIds.remove(track.id)
                        else likedIds.add(track.id)
                    },

                    onRepeat = {
                        onShowRepeat(true)
                    },

                    onAutoplay = {
                        vm.toggleAutoplay()
                    },

                    onQueue = {
                        onShowQueue(true)
                    },

                    onDownload = {
                        downloadTrack(track)
                    },

                    onCancelDownload = {
                        vm.cancelDownload(track)
                    },

                    onDelete = {
                        onDeleteTarget(track)
                    }
                )
            )
        } else {
            PlayerPlaceholder(
                state.loadingText,
                state.error
            ) {
                onShowPlayer(false)
            }
        }
    }

    AnimatedVisibility(
        visible = state.lyricsVisible,
        enter = fadeIn(tween(240)),
        exit = fadeOut(tween(180))
    ) {
        RexLyricsOverlay(
            track = state.nowPlaying
                ?: RexTrack(
                    id = "",
                    title = "",
                    artist = ""
                ),
            lyrics = state.lyrics,
            loading = state.lyricsLoading,
            error = state.lyricsError,
            accent = null,
            positionState = positionState,
            durationMs = state.durationMs,
            isPlaying = state.isPlaying,
            onClose = {
                vm.closeLyrics()
            },
            onSeekTo = { vm.seekToPosition(it) }
        )
    }
}
