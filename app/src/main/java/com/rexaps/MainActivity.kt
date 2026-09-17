package com.rexaps

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.rexaps.ui.RexApsApp

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RexApsApp(activity = this@MainActivity)
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        super.onBackPressed()
    }
}
