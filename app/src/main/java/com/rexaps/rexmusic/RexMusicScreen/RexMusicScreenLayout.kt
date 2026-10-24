package com.rexaps.rexmusic

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
internal fun RexMusicScreenLayout(
    state: RexMusicUiState,
    tab: Int,
    actions: RexActions,
    homeList: androidx.compose.foundation.lazy.LazyListState,
    searchList: androidx.compose.foundation.lazy.LazyListState,
    playlistList: androidx.compose.foundation.lazy.LazyListState,
    libraryList: androidx.compose.foundation.lazy.LazyListState,
    onBack: () -> Unit,
    query: String,
    typing: Boolean,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    selectedPlaylistName: String?,
    onOpenPlaylist: (String) -> Unit,
    onBackFromPlaylist: () -> Unit,
    onRequestDeletePlaylist: (String) -> Unit,
    libTab: Int,
    onLibTabChange: (Int) -> Unit,
    onTabSelected: (Int) -> Unit,
    onReturnHome: () -> Unit,
    miniPlayer: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Base the layout on this window's available bounds, not device configuration.
        // This also responds when the app is resized or placed in split-screen.
        val useLandscapeLayout = maxWidth > maxHeight

        val screenContent: @Composable (Modifier) -> Unit = { contentModifier ->
            AnimatedContent(
                targetState = tab,
                modifier = contentModifier,
                transitionSpec = {
                    (fadeIn(tween(230, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { it / 22 }) togetherWith
                        (fadeOut(tween(150)) +
                            slideOutHorizontally(tween(190, easing = FastOutSlowInEasing)) { -it / 30 })
                },
                label = "RexMusicTabTransition"
            ) { visibleTab ->
                Box(Modifier.fillMaxSize()) {
                    when (visibleTab) {
                        TAB_HOME -> {
                            HomeTab(
                                state,
                                actions,
                                homeList,
                                onBack
                            )
                        }

                        TAB_SYNC -> {
                            RexMusicSyncScreen(onBack = onReturnHome)
                        }

                        TAB_SEARCH -> {
                            SearchTab(
                                state = state,
                                query = query,
                                typing = typing && !state.offline.enabled,
                                onQueryChange = onQueryChange,
                                onClear = onClearSearch,
                                actions = actions,
                                listState = searchList
                            )
                        }

                        TAB_PLAYLIST -> {
                            PlaylistTab(
                                state = state,
                                actions = actions,
                                listState = playlistList,
                                selectedPlaylistName = selectedPlaylistName,
                                onOpenPlaylist = onOpenPlaylist,
                                onBackFromDetail = onBackFromPlaylist,
                                onRequestDeletePlaylist = onRequestDeletePlaylist
                            )
                        }

                        else -> {
                            LibraryTab(
                                state,
                                libTab,
                                onLibTabChange,
                                actions,
                                libraryList
                            )
                        }
                    }

                    ErrorCard(
                        error = state.error,
                        onDismiss = actions.dismissError,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }

        if (useLandscapeLayout) {
            RexMusicScreenLandscape(
                selectedTab = tab,
                onTabSelected = onTabSelected,
                miniPlayer = miniPlayer,
                snackbarHost = snackbarHost,
                content = screenContent
            )
        } else {
            RexMusicScreenPortrait(
                selectedTab = tab,
                onTabSelected = onTabSelected,
                miniPlayer = miniPlayer,
                snackbarHost = snackbarHost,
                content = screenContent
            )
        }
    }
}
