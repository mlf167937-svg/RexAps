package com.rexaps

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import com.rexaps.ui.RexApsApp
import com.rexaps.ui.SplashScreen

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasSeenSplash = prefs.getBoolean("has_seen_splash", false)

        setContent {
            // Splash cuma muncul kalau belum pernah lihat
            var showSplash by remember { mutableStateOf(!hasSeenSplash) }

            if (showSplash) {
                SplashScreen(onFinished = {
                    // Tandai splash sudah pernah dilihat
                    prefs.edit().putBoolean("has_seen_splash", true).apply()
                    showSplash = false
                })
            } else {
                RexApsApp(activity = this@MainActivity)
            }
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        super.onBackPressed()
    }
}