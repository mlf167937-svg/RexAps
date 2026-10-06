package com.rexaps.rexmonitor

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rexmonitor.ui.RexMonitorHome
import com.rexaps.rexmonitor.ui.RexMonitorTheme
import com.rexaps.rexmonitor.ui.RexTheme

/** Entry point modul. Shell `com.rexaps.ui` cukup memanggil composable ini. */
@Composable
fun RexMonitorScreen(
    modifier: Modifier = Modifier,
    viewModel: RexMonitorViewModel = viewModel()
) {
    RexMonitorTheme {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val p = RexTheme.palette

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted -> viewModel.onPermissionResult(granted) }

        // Pengguna mungkin mengubah izin di Settings lalu kembali.
        LifecycleResumeEffect(Unit) {
            viewModel.refreshPermission()
            onPauseOrDispose { }
        }

        fun openNotificationSettings() {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }

        Box(
            modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(p.bgTop, p.bgBottom)))
        ) {
            RexMonitorHome(
                state = state,
                onToggleMonitoring = viewModel::onMonitoringToggle,
                onOptimize = viewModel::optimize,
                onRequestRoot = viewModel::requestRoot
            )
        }

        when (state.permissionDialog) {
            RexPermissionDialog.RATIONALE -> AlertDialog(
                onDismissRequest = viewModel::dismissDialog,
                title = { Text("Notification permission required") },
                text = {
                    Text("RexMonitor needs notification permission to display device monitoring information.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.dismissDialog()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            openNotificationSettings()
                        }
                    }) { Text("Allow") }
                },
                dismissButton = { TextButton(onClick = viewModel::dismissDialog) { Text("Cancel") } }
            )
            RexPermissionDialog.DENIED -> AlertDialog(
                onDismissRequest = viewModel::dismissDialog,
                title = { Text("Monitoring notification is unavailable.") },
                text = { Text("Notifikasi dinonaktifkan untuk RexAps. Aktifkan di Settings bila ingin memakai fitur ini.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.dismissDialog()
                        openNotificationSettings()
                    }) { Text("Open Settings") }
                },
                dismissButton = { TextButton(onClick = viewModel::dismissDialog) { Text("Cancel") } }
            )
            RexPermissionDialog.NONE -> Unit
        }
    }
}
