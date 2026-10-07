package com.rexaps.rexcoder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.toArgb
import com.rexaps.rexcoder.theme.Rex
import com.rexaps.rexcoder.ui.RexCoderApp
import com.rexaps.rexcoder.storage.WorkspaceManager

class RexCoderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWorkspaceAccess()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Rex.DarkTitleBar.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Rex.DarkActivityBar.toArgb())
        )
        setContent { RexCoderApp() }
    }

    private fun requestWorkspaceAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                runCatching {
                    startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    })
                }
            }
        } else {
            val needed = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (needed.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
                ActivityCompat.requestPermissions(this, needed, 7001)
            }
        }
        WorkspaceManager.ensureRoot()
    }
}
