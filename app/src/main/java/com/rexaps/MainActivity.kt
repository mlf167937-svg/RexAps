
package com.rexaps

import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.compose.runtime.*
import com.rexaps.ui.RexApsApp
import com.rexaps.ui.SplashScreen

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var showSplash by remember {
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
                    activity = this@MainActivity
                )
            }
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        super.onBackPressed()
    }
}
