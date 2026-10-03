package com.rexaps.rexchat

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

/** Normal mode: WebView Profiles, same process, sessions can run side by side. */
open class WaActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SESSION_ID = "rexchat_session_id"
        const val EXTRA_METHOD = "rexchat_method" // "", "qr", "pair"
    }

    protected open val legacy = false

    private var controller: WaController? = null
    private var pickMime = "*/*"

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> controller?.onFilesPicked(uris) }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        controller?.onPermissionsResult(result.isNotEmpty() && result.values.all { it })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // ALWAYS first

        val id = intent.getIntExtra(EXTRA_SESSION_ID, -1)
        val session = if (id > 0) SessionManager.get(this, id) else null
        if (session == null) {
            finishAndRemoveTask()
            return
        }

        if (legacy) {
            val ok = runCatching {
                WebView.setDataDirectorySuffix(SessionManager.suffix(id))
            }.isSuccess
            if (!ok) {
                Toast.makeText(this, "Closing previous session, please tap again.", Toast.LENGTH_SHORT).show()
                finishAndRemoveTask()
                Process.killProcess(Process.myPid())
                return
            }
        }

        SessionManager.touch(this, id)

        val c = WaController(this, id, legacy, intent.getStringExtra(EXTRA_METHOD).orEmpty())
        c.onPickFiles = { mime -> pickMime = mime; filePicker.launch(mime) }
        c.onNeedPermissions = { perms -> permissionLauncher.launch(perms) }
        controller = c

        setContent {
            RexTheme {
                WaScreen(
                    c = c,
                    title = session.name,
                    onExit = { exitSession() },
                    onDeleteSession = {
                        SessionManager.markPendingDelete(this, id)
                        exitSession()
                    },
                    onOpenWhatsApp = { openWhatsApp() }
                )
            }
        }
    }

    private fun openWhatsApp() {
        val pm = packageManager
        val intent = listOf("com.whatsapp", "com.whatsapp.w4b")
            .firstNotNullOfOrNull { pm.getLaunchIntentForPackage(it) }
        if (intent != null) startActivity(intent)
        else Toast.makeText(this, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
    }

    private fun exitSession() {
        controller?.destroy()
        controller = null
        finishAndRemoveTask()
        if (legacy) {
            window.decorView.postDelayed({ Process.killProcess(Process.myPid()) }, 200L)
        }
    }

    override fun onPause() { super.onPause(); controller?.onPause() }
    override fun onResume() { super.onResume(); controller?.onResume() }

    override fun onDestroy() {
        controller?.destroy()
        super.onDestroy()
    }
}

/** Fallback for old WebViews without Profile support: one process per session. */
class WaLegacyActivity : WaActivity() {
    override val legacy = true
}