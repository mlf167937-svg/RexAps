package com.rexaps.rexmanager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

// ───────────────────────── Entry ─────────────────────────
@Composable
fun RexManagerApp(onExit: () -> Unit = {}) {
    var splash by rememberSaveable { mutableStateOf(true) }
    if (splash) {
        RexSplashManager(onStart = { splash = false }, onExit = onExit)
    } else {
        RexManagerScreen(onExit = onExit)
    }
}
