package com.rexaps.rexwarp

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.VpnService
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rexwarp.ui.*
import com.rexaps.rexwarp.ui.components.RexWarpAnimatedBackdrop
import java.time.LocalDate
import androidx.compose.ui.graphics.vector.ImageVector

internal enum class Dest(val title: String, val navLabel: String, val icon: ImageVector?) {
    HOME("RexWARP", "Home", Icons.Filled.Home),
    USAGE("Usage", "Usage", Icons.Filled.DateRange),
    HISTORY("Usage History", "History", Icons.AutoMirrored.Filled.List),
    SETTINGS("Settings", "Settings", Icons.Filled.Settings),
    SPEED_TEST("Cloudflare Speed Test", "Speed Test", Icons.Filled.Speed),
    DETAILS("Connection Details", "Details", null),
    ABOUT("About & Privacy", "About", null)
}

/** Entry point for the RexAps shell. RexWARP keeps the host color palette and applies local shape tokens. */
@Composable
fun RexWarpScreen(
    modifier: Modifier = Modifier,
    onExit: (() -> Unit)? = null,
    viewModel: RexWarpViewModel = viewModel()
) {
    RexWarpTheme {
        RexWarpScreenContent(modifier, onExit, viewModel)
    }
}

@Composable
private fun RexWarpScreenContent(
    modifier: Modifier,
    onExit: (() -> Unit)?,
    viewModel: RexWarpViewModel
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val periods by viewModel.periods.collectAsStateWithLifecycle()
    val usageUi by viewModel.usageUi.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val allUsage by viewModel.allUsage.collectAsStateWithLifecycle()

    var dest by rememberSaveable { mutableStateOf(Dest.HOME) }
    var askedNotification by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshToday() }

    fun goBack() {
        when (dest) {
            Dest.HOME -> onExit?.invoke()
            Dest.ABOUT -> dest = Dest.SETTINGS
            else -> dest = Dest.HOME
        }
    }
    BackHandler(enabled = dest != Dest.HOME || onExit != null, onBack = ::goBack)

    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.startVpn() else viewModel.onPermissionDenied()
    }
    fun proceedVpn() {
        val intent = VpnService.prepare(context)
        if (intent != null) vpnLauncher.launch(intent) else viewModel.startVpn()
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { proceedVpn() }

    val onConnectClick: () -> Unit = {
        when (state.connection) {
            RexWarpConnectionState.DISCONNECTED, RexWarpConnectionState.ERROR ->
                if (state.pausedReason != null) viewModel.disconnect()
                else if (Build.VERSION.SDK_INT >= 33 && !askedNotification &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    askedNotification = true
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else proceedVpn()
            RexWarpConnectionState.CONNECTING, RexWarpConnectionState.CONNECTED -> viewModel.disconnect()
            RexWarpConnectionState.DISCONNECTING -> Unit
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(viewModel::export) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(viewModel::importUsage) }

    val navSelected = when (dest) { Dest.DETAILS -> Dest.HOME; Dest.ABOUT -> Dest.SETTINGS; else -> dest }
    val navItems = listOf(Dest.HOME, Dest.USAGE, Dest.HISTORY, Dest.SPEED_TEST, Dest.SETTINGS)
    val pageContent: @Composable (Boolean) -> Unit = { roomy ->
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
            Box(Modifier.widthIn(max = if (roomy) 1_280.dp else 840.dp).fillMaxSize()) {
                AnimatedContent(
                    targetState = dest,
                    transitionSpec = {
                        (fadeIn(tween(240)) + slideInHorizontally(tween(240)) { it / 24 }) togetherWith
                            (fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { -it / 32 })
                    },
                    label = "rexwarp-page-transition"
                ) { page ->
                    when (page) {
                        Dest.HOME -> RexWarpHome(
                            state = state, periods = periods, hasUsage = usageUi?.hasAnyData == true, wide = roomy,
                            onConnectClick = onConnectClick, onOpenDetails = { dest = Dest.DETAILS }
                        )
                        Dest.USAGE -> RexWarpUsageScreen(
                            ui = usageUi, onPreset = viewModel::selectPreset, onGraph = viewModel::selectGraph,
                            onCustomRange = viewModel::setCustomRange
                        )
                        Dest.HISTORY -> {
                            val historyHourly by viewModel.historyHourly.collectAsStateWithLifecycle()
                            RexWarpHistoryScreen(rows = history, graphRows = allUsage, hourlyRows = historyHourly)
                        }
                        Dest.SPEED_TEST -> RexWarpSpeedTestScreen(
                            vpnConnected = when (state.connection) {
                                RexWarpConnectionState.CONNECTED -> true
                                RexWarpConnectionState.DISCONNECTED, RexWarpConnectionState.ERROR -> false
                                RexWarpConnectionState.CONNECTING, RexWarpConnectionState.DISCONNECTING -> null
                            }
                        )
                        Dest.SETTINGS -> RexWarpSettingsScreen(
                            settings = settings, capabilities = viewModel.capabilities,
                            engineReason = state.engineUnavailableReason,
                            onChange = viewModel::saveSettings,
                            onExport = { exportLauncher.launch("rexwarp-usage-${LocalDate.now()}.json") },
                            onImport = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                            onResetToday = viewModel::resetToday, onResetAll = viewModel::resetAll,
                            onOpenAbout = { dest = Dest.ABOUT }
                        )
                        Dest.DETAILS -> RexWarpDetailsScreen(state, onConnectClick = onConnectClick)
                        Dest.ABOUT -> RexWarpAboutScreen()
                    }
                }
            }
        }
    }

    val showBack = dest == Dest.DETAILS || dest == Dest.ABOUT || onExit != null
    Box(modifier.fillMaxSize()) {
        RexWarpAnimatedBackdrop(Modifier.matchParentSize())
        if (landscape) {
            RexWarpScreenLandscape(
                title = dest.title, showBack = showBack, onBack = ::goBack,
                navItems = navItems, navSelected = navSelected, onNavigate = { dest = it },
                snackbarHostState = snackbar, content = pageContent
            )
        } else {
            RexWarpScreenPortrait(
                title = dest.title, showBack = showBack, onBack = ::goBack,
                navItems = navItems, navSelected = navSelected, onNavigate = { dest = it },
                snackbarHostState = snackbar, content = pageContent
            )
        }
    }
}
