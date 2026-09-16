package com.rexaps

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = TextView(this).apply {
            text = "RexAps v0.1"
            textSize = 28f
            setPadding(32, 32, 32, 32)
        }

        setContentView(text)
    }
}