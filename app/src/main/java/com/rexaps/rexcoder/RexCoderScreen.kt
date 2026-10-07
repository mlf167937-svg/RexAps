package com.rexaps.rexcoder

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.rexaps.rexcoder.ui.RexCoderApp

@Composable
fun RexCoderScreen(activity: Activity, onExit: () -> Unit) {
    BackHandler(enabled = true) { onExit() }
    Box(Modifier.fillMaxSize().background(Color.Transparent)) {
        RexCoderApp(Modifier.fillMaxSize())
    }
}
