package com.rexaps.rexfox

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class RexFox(private val activity: Activity) {
    fun start() {
        val componentActivity = activity as? ComponentActivity ?: return
        componentActivity.setContent {
            RexFoxScreen(activity = activity)
        }
    }
}
