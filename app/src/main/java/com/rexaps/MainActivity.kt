
package com.rexaps

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import com.rexaps.ui.RexApsApp
import com.rexaps.ui.SplashScreen

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Terapkan theme sebelum Activity dibuat.
        // Background awal diatur melalui styles.xml.
        super.onCreate(savedInstanceState)

        setContent {
            // Splash selalu muncul setiap MainActivity dibuat.
            var showSplash by remember {
                mutableStateOf(true)
            }

            if (showSplash) {
                SplashScreen(
                    onFinished = {
                        showSplash = false
                    }
                )
            } else {
                // Logika dan halaman utama tetap menggunakan
                // implementasi RexAps yang sudah ada.
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
