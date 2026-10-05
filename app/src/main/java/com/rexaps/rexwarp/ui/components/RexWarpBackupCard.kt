package com.rexaps.rexwarp.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rexwarp.RexWarpViewModel

/** Pakai ViewModel yang sama dengan RexWarpScreen (scope Activity), jadi tidak perlu mengubah RexWarpScreen. */
@Composable
fun RexWarpBackupCard(modifier: Modifier = Modifier, viewModel: RexWarpViewModel = viewModel()) {
    val ui by viewModel.backup.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshBackup() }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { it?.let(viewModel::setBackupFolder) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.setRecording(true) }

    fun toggle(on: Boolean) {
        if (on && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else viewModel.setRecording(on)
    }

    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Usage recording & backup", style = MaterialTheme.typography.titleMedium)

            Row(
                Modifier.fillMaxWidth().toggleable(value = ui.recording, role = Role.Switch, onValueChange = ::toggle).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Record usage every minute", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Counts all device traffic, even when WARP is off. Shows a small notification while recording.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = ui.recording, onCheckedChange = null)
            }

            HorizontalDivider()

            Text("Backup folder", style = MaterialTheme.typography.labelLarge)
            Text(
                ui.folderName ?: "Not set",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Choose Download/RexAps/RexWARP/DATA (create it in the picker). Daily CSV files stay on your phone after uninstall.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickFolder.launch(null) }) { Text(if (ui.folderName == null) "Choose folder" else "Change folder") }
                OutlinedButton(enabled = ui.folderName != null, onClick = viewModel::restoreFromFolder) { Text("Restore") }
            }
        }
    }
}