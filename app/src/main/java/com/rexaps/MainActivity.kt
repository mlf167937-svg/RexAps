package com.rexaps

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.rexaps.rexfox.RexFox
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
                    openRexFox()
                }
            )
        }
    }

    private fun openRexFox() {
        val rexFox = RexFox(this)
        rexFox.start()
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        showHome()
    }
}
