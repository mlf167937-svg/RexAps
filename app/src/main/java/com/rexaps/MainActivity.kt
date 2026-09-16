package com.rexaps

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rexaps.rexfox.RexFox

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rexFox = RexFox(this)
        rexFox.start()
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        super.onBackPressed()
    }
}
