package com.rexaps.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.rexaps.rexfox.RexFoxScreen
import com.rexaps.rexfox.RexFoxTheme

@Composable
fun RexApsApp() {
    RexFoxTheme {
        val context = LocalContext.current as? Activity
        if (context != null) {
            RexFoxScreen(activity = context)
        }
    }
}
