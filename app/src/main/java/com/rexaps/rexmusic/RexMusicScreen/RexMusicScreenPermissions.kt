package com.rexaps.rexmusic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

internal data class RexMusicPermissionActions(
    val requestNotifications: () -> Unit,
    val openStorageSettings: () -> Unit,
    val downloadTrack: (RexTrack) -> Unit
)

@Composable
internal fun rememberRexMusicPermissionActions(
    vm: RexMusicViewModel,
    state: RexMusicUiState,
    pendingDownload: RexTrack?,
    onPendingDownload: (RexTrack?) -> Unit,
    onShowStorageDialog: (Boolean) -> Unit
): RexMusicPermissionActions {
    val context = LocalContext.current
val notifLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) { }

val requestNotifications: () -> Unit = {
    if (
        Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(
            context,
            NOTIF_PERMISSION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        notifLauncher.launch(NOTIF_PERMISSION)
    }
}

val settingsLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) {
    vm.refreshOffline()
}

val storageLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) {
    vm.refreshOffline()
}

val openStorageSettings: () -> Unit = {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val appIntent = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )

        try {
            settingsLauncher.launch(appIntent)
        } catch (_: Exception) {
            settingsLauncher.launch(
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            )
        }
    } else {
        storageLauncher.launch(
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}

LaunchedEffect(state.offline.hasAccess) {
    val pending = pendingDownload

    if (state.offline.hasAccess && pending != null) {
        onPendingDownload(null)
        onShowStorageDialog(false)
        vm.download(pending)
    }
}

val downloadTrack: (RexTrack) -> Unit = { track ->
    requestNotifications()
    if (RexOfflineManager.hasAccess(context)) {
        vm.download(track)
    } else {
        onPendingDownload(track)
        onShowStorageDialog(true)
    }
}
    return RexMusicPermissionActions(requestNotifications, openStorageSettings, downloadTrack)
}
