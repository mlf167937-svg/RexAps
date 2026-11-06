
package com.rexaps

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.compose.runtime.*
import com.rexaps.ui.ACTION_OPEN_REX_MODULE
import com.rexaps.ui.EXTRA_REX_MODULE_ID
import com.rexaps.ui.RexApsApp
import com.rexaps.ui.SplashScreen

class MainActivity : AppCompatActivity() {

    private var pendingShortcutModuleId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingShortcutModuleId = moduleIdFrom(intent)

        setContent {
            var showSplash by androidx.compose.runtime.saveable.rememberSaveable {
                mutableStateOf(true)
            }

            if (showSplash) {
                SplashScreen(
                    onFinished = {
                        // Kembalikan system bar ke tema gelap
                        // setelah splash selesai.
                        window.statusBarColor = Color.BLACK
                        window.navigationBarColor = Color.BLACK

                        WindowCompat.getInsetsController(
                            window,
                            window.decorView
                        ).apply {
                            // Ikon status bar putih
                            isAppearanceLightStatusBars = false

                            // Garis navigation bar tetap putih
                            isAppearanceLightNavigationBars = false
                        }

                        showSplash = false
                    }
                )
            } else {
                RexApsApp(
                    activity = this@MainActivity,
                    initialModuleId = pendingShortcutModuleId,
                    onShortcutConsumed = { pendingShortcutModuleId = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingShortcutModuleId = moduleIdFrom(intent)
    }

    private fun moduleIdFrom(source: Intent?): String? {
        if (source?.action != ACTION_OPEN_REX_MODULE) return null
        return source.getStringExtra(EXTRA_REX_MODULE_ID)
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        super.onBackPressed()
    }
}
