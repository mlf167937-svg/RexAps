package com.rexaps

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.rexaps.ui.RexApsApp

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showHome()
    }

    private fun showHome() {
        setContent {
            RexApsApp(
                onOpenRexFox = {
                    // RexFox akan kita sambungkan di langkah berikutnya.
                }
            )
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        showHome()
    }
}
