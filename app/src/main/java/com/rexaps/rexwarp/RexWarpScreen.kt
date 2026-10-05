package com.rexaps.rexwarp

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rexwarp.ui.*
import java.time.LocalDate

private enum class Dest(val title: String, val navLabel: String, val icon: ImageVector?) {
    HOME("RexWARP", "Home", Icons.Filled.Home),
    USAGE("Usage", "Usage", Icons.Filled.DateRange),
    HISTORY("Usage History", "History", Icons.AutoMirrored.Filled.List),
    SETTINGS("Settings", "Settings", Icons.Filled.Settings),
    DETAILS("Connection Details", "Details", null),
    ABOUT("About & Privacy", "About", null)
}

/** Satu-satunya entry point yang dipanggil main shell RexAps. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexWarpScreen(
    modifier: Modifier = Modifier,
    onExit: (() -> Unit)? = null,
    viewModel: RexWarpViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val periods by viewModel.periods.collectAsStateWithLifecycle()
    val usageUi by viewModel.usageUi.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    var dest by rememberSaveable { mutableStateOf(Dest.HOME) }
    var askedNotification by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshToday() }

    // Back sistem: sub-halaman -> induknya, Home -> keluar ke shell RexAps
    BackHandler(enabled = dest != Dest.HOME || onExit != null) {
        when (dest) {
            Dest.HOME -> onExit?.invoke()
            Dest.ABOUT -> dest = Dest.SETTINGS
            else -> dest = Dest.HOME
        }
    }

    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) viewModel.startVpn() else viewModel.onPermissionDenied()
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
                ) { askedNotification = true; notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                else proceedVpn()
            RexWarpConnectionState.CONNECTING, RexWarpConnectionState.CONNECTED -> viewModel.disconnect()
            RexWarpConnectionState.DISCONNECTING -> Unit
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(viewModel::export) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(viewModel::importUsage) }

    val navSelected = when (dest) { Dest.DETAILS -> Dest.HOME; Dest.ABOUT -> Dest.SETTINGS; else -> dest }
    val navItems = listOf(Dest.HOME, Dest.USAGE, Dest.HISTORY, Dest.SETTINGS)

    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        val twoPane = maxWidth >= 840.dp
        Row(Modifier.fillMaxSize()) {
            if (wide) NavigationRail {
                Spacer(Modifier.weight(1f))
                navItems.forEach { d ->
                    NavigationRailItem(
                        selected = navSelected == d, onClick = { dest = d },
                        icon = { Icon(d.icon!!, contentDescription = null) }, label = { Text(d.navLabel) }
                    )
                }
                Spacer(Modifier.weight(1f))
            }
            Scaffold(
                modifier = Modifier.weight(1f),
                snackbarHost = { SnackbarHost(snackbar) },
                topBar = {
                    TopAppBar(
                        title = { Text(dest.title) },
                        navigationIcon = {
                            val sub = dest == Dest.DETAILS || dest == Dest.ABOUT
                            if (sub || onExit != null) IconButton(onClick = {
                                if (sub) dest = if (dest == Dest.ABOUT) Dest.SETTINGS else Dest.HOME
                                else onExit?.invoke()
                            }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                        }
                    )
                },
                bottomBar = {
                    if (!wide) NavigationBar {
                        navItems.forEach { d ->
                            NavigationBarItem(
                                selected = navSelected == d, onClick = { dest = d },
                                icon = { Icon(d.icon!!, contentDescription = null) }, label = { Text(d.navLabel) }
                            )
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.widthIn(max = 840.dp).fillMaxSize()) {
                        when (dest) {
                            Dest.HOME -> RexWarpHome(
                                state = state, periods = periods, hasUsage = usageUi?.hasAnyData == true, wide = twoPane,
                                onConnectClick = onConnectClick, onOpenDetails = { dest = Dest.DETAILS }
                            )
                            Dest.USAGE -> RexWarpUsageScreen(
                                ui = usageUi, onPreset = viewModel::selectPreset, onGraph = viewModel::selectGraph,
                                onCustomRange = viewModel::setCustomRange
                            )
                            Dest.HISTORY -> RexWarpHistoryScreen(rows = history)
                            Dest.SETTINGS -> RexWarpSettingsScreen(
                                settings = settings, capabilities = viewModel.capabilities,
                                engineReason = state.engineUnavailableReason,
                                onChange = viewModel::saveSettings,
                                onExport = { exportLauncher.launch("rexwarp-usage-${LocalDate.now()}.json") },
                                onImport = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                                onResetToday = viewModel::resetToday, onResetAll = viewModel::resetAll,
                                onOpenAbout = { dest = Dest.ABOUT }
                            )
                            Dest.DETAILS -> RexWarpDetailsScreen(state)
                            Dest.ABOUT -> RexWarpAboutScreen()
                        }
                    }
                }
            }
        }
    }
}