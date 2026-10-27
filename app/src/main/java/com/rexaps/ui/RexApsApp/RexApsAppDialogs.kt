package com.rexaps.ui

import android.app.Activity
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.WindowCompat
import com.rexaps.rexlife.RexLifeScreen
import com.rexaps.rexcoder.RexCoderScreen
import com.rexaps.rexfox.RexFoxScreen
import com.rexaps.rexgit.RexGit
import com.rexaps.rexmanager.RexManagerScreen
import com.rexaps.rexmusic.RexMusicScreen
import com.rexaps.rexnux.RexNuxScreen
import com.rexaps.rexspacex.RexSpaceXScreen
import com.rexaps.rextools.navigation.RexNavGraph

@Composable
internal fun __splitImpl_RexApsApp(activity: Activity) {

    var route by remember { mutableStateOf(RexRoute.NONE) }
    var routeLoading by remember { mutableStateOf(false) }

    val themeStore = remember { RexThemeStore(activity) }
    var themeOption by remember { mutableStateOf(themeStore.load()) }

    var themeApplying by remember { mutableStateOf(false) }
    var pendingTheme by remember { mutableStateOf<RexThemeOption?>(null) }
    var showThemeSheet by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(RexTab.HOME) }

    // System bars belong to RexAps, not the Android device's light/dark preference.
    val activePalette = remember(themeOption) { RexPaletteSpec.of(themeOption) }
    SideEffect {
        activity.window.statusBarColor = Color(activePalette.background).toArgb()
        activity.window.navigationBarColor = Color(activePalette.background).toArgb()
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !activePalette.dark
            isAppearanceLightNavigationBars = !activePalette.dark
        }
    }

    // A module without its own nested-back action returns to the RexAps shell instead of closing the app.
    BackHandler(enabled = route != RexRoute.NONE) {
        route = RexRoute.NONE
        routeLoading = false
    }

    val onThemeChange: (RexThemeOption) -> Unit = { option ->
        if (option != themeOption) {
            pendingTheme = option
            themeApplying = true
        }
    }

    /* SUB-APP ROUTES */
    if (route != RexRoute.NONE) {

        val appName = route.displayName

        RexTheme(option = themeOption) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground
            ) {
                Box(Modifier.fillMaxSize()) {
                    // Keep a single theme-aware animated backdrop underneath every sub-module.
                    ProAmbientBackdrop(Modifier.fillMaxSize())
                    Crossfade(
                        targetState = routeLoading,
                        animationSpec = tween(320),
                        label = "routeTransition"
                    ) { isLoading ->
                        if (isLoading) {
                            RexAppLoader(
                                appName = appName,
                                onFinished = { routeLoading = false }
                            )
                        } else {
                            when (route) {
                                RexRoute.REXLIFE -> RexLifeScreen(onExit = { route = RexRoute.NONE })

                                RexRoute.REXFOX -> RexFoxScreen(activity = activity)

                                RexRoute.REXPANEL ->
                                    com.rexaps.rexpanel.RexPanelScreen(
                                        activity = activity,
                                        onExit = { route = RexRoute.NONE }
                                    )

                                RexRoute.REXCHAT ->
                                    com.rexaps.rexchat.SessionListScreen(
                                        onOpen = { session, method ->
                                            com.rexaps.rexchat.openRexChatSession(activity, session, method)
                                        },
                                        onExit = { route = RexRoute.NONE }
                                    )

                                RexRoute.REXNUX ->
                                    RexNuxScreen(onExit = { route = RexRoute.NONE })

                                RexRoute.REXTOOLS ->
                                    RexNavGraph(onBack = { route = RexRoute.NONE })

                                RexRoute.REXMUSIC ->
                                    RexMusicScreen(
                                        onBack = { route = RexRoute.NONE },
                                        onOpenLocalProfile = {
                                            route = RexRoute.NONE
                                            routeLoading = false
                                            selectedTab = RexTab.ACCOUNT
                                        }
                                    )

                                RexRoute.REXMANAGER ->
                                    RexManagerScreen(onExit = { route = RexRoute.NONE })

                                RexRoute.REXGIT -> RexGit()

                                RexRoute.REXCODER ->
                                    RexCoderScreen(
                                        activity = activity,
                                        onExit = { route = RexRoute.NONE }
                                    )

                                RexRoute.REXMONITOR ->
                                    com.rexaps.rexmonitor.RexMonitorScreen(
                                        onExit = { route = RexRoute.NONE }
                                    )

                                RexRoute.REXSPACEX ->
                                    RexSpaceXScreen(onExit = { route = RexRoute.NONE })

                                RexRoute.REXWARP ->
                                    com.rexaps.rexwarp.RexWarpScreen(
                                        onExit = { route = RexRoute.NONE }
                                    )

                                RexRoute.NONE -> Unit
                            }
                        }
                    }
                }
            }
        }
        return
    }

    /* MAIN SHELL — portrait / landscape */
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val shell = RexShell(
        selectedTab = selectedTab,
        themeOption = themeOption,
        onSelectTab = { selectedTab = it },
        onAppClick = { app ->
            if (app.available) {
                RexRoute.fromModuleId(app.id)?.let {
                    route = it
                    routeLoading = true
                }
            }
        },
        onOpenTheme = { showThemeSheet = true }
    )

    RexTheme(option = themeOption) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            Box(Modifier.fillMaxSize()) {
                if (isLandscape) {
                    RexAppsScreenLandscape(shell)
                } else {
                    RexAppsScreenPortrait(shell)
                }

                if (showThemeSheet) {
                    ThemeSheet(
                        selected = themeOption,
                        onSelect = onThemeChange,
                        onDismiss = { showThemeSheet = false }
                    )
                }

                AnimatedVisibility(
                    visible = themeApplying,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(150))
                ) {
                    pendingTheme?.let { target ->
                        RexThemeLoadingOverlay(
                            targetOption = target,
                            onFinished = {
                                themeOption = target
                                themeStore.save(target)
                                pendingTheme = null
                                themeApplying = false
                                showThemeSheet = false
                            }
                        )
                    }
                }
            }
        }
    }
}
